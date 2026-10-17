#!/bin/bash
# Installs the monitoring stack on the oskarro.com server (Debian 12). Safe to re-run.
#   Prometheus (127.0.0.1:9090, 30 days) <- node exporter (:9100), nginx exporter (:9113), backend /actuator/prometheus
#   Grafana    (127.0.0.1:3000) served by nginx at https://oskarro.com/grafana/
# Needs GRAFANA_USER and GRAFANA_PASSWORD in the environment (from the secrets folder).
# The "Oskarro — przegląd" dashboard (ops/grafana-oskarro-overview.json) is copied to
# /var/lib/grafana/dashboards/ separately and set as the Grafana home page.
set -euo pipefail
: "${GRAFANA_USER:?}" "${GRAFANA_PASSWORD:?}"
export DEBIAN_FRONTEND=noninteractive

echo "== nginx stub_status for the exporter (localhost only)"
cat > /etc/nginx/conf.d/oskarro-stub-status.conf <<'NGX'
server {
    listen 127.0.0.1:8090;
    location = /nginx_status {
        stub_status;
        allow 127.0.0.1;
        deny all;
    }
}
NGX
nginx -t -q && systemctl reload nginx

echo "== exporters and Prometheus bound to localhost"
echo 'ARGS="--web.listen-address=127.0.0.1:9100"' > /etc/default/prometheus-node-exporter
echo 'ARGS="-nginx.scrape-uri=http://127.0.0.1:8090/nginx_status -web.listen-address=127.0.0.1:9113"' > /etc/default/prometheus-nginx-exporter
echo 'ARGS="--web.listen-address=127.0.0.1:9090 --storage.tsdb.retention.time=30d"' > /etc/default/prometheus
cat > /etc/prometheus/prometheus.yml <<'PROM'
global:
  scrape_interval: 15s
  evaluation_interval: 15s
scrape_configs:
  - job_name: prometheus
    static_configs:
      - targets: ['127.0.0.1:9090']
  - job_name: node
    static_configs:
      - targets: ['127.0.0.1:9100']
  - job_name: nginx
    static_configs:
      - targets: ['127.0.0.1:9113']
  - job_name: oskarro
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['127.0.0.1:8081']
PROM
for unit in prometheus prometheus-node-exporter prometheus-nginx-exporter; do
  systemctl unmask "$unit" >/dev/null 2>&1 || true
  systemctl reset-failed "$unit" >/dev/null 2>&1 || true
  systemctl enable -q "$unit"
  systemctl restart "$unit"
done

echo "== Grafana (official apt repository)"
if ! dpkg -s grafana >/dev/null 2>&1; then
  apt-get -qq install -y apt-transport-https gnupg wget >/dev/null
  mkdir -p /etc/apt/keyrings
  wget -qO- https://apt.grafana.com/gpg.key | gpg --dearmor > /etc/apt/keyrings/grafana.gpg
  echo "deb [signed-by=/etc/apt/keyrings/grafana.gpg] https://apt.grafana.com stable main" > /etc/apt/sources.list.d/grafana.list
  apt-get -qq update
  apt-get -qq install -y grafana >/dev/null
fi

sed -i '/^GF_/d' /etc/default/grafana-server
cat >> /etc/default/grafana-server <<GF
GF_SERVER_HTTP_ADDR=127.0.0.1
GF_SERVER_HTTP_PORT=3000
GF_SERVER_ROOT_URL=https://oskarro.com/grafana/
GF_SERVER_SERVE_FROM_SUB_PATH=true
GF_SECURITY_ADMIN_USER=${GRAFANA_USER}
GF_SECURITY_COOKIE_SECURE=true
GF_USERS_ALLOW_SIGN_UP=false
GF_AUTH_ANONYMOUS_ENABLED=false
GF_ANALYTICS_REPORTING_ENABLED=false
GF_ANALYTICS_CHECK_FOR_UPDATES=false
GF_NEWS_NEWS_FEED_ENABLED=false
GF
chmod 640 /etc/default/grafana-server
chown root:grafana /etc/default/grafana-server

mkdir -p /etc/grafana/provisioning/datasources /etc/grafana/provisioning/dashboards /var/lib/grafana/dashboards
cat > /etc/grafana/provisioning/datasources/prometheus.yml <<'DS'
apiVersion: 1
datasources:
  - name: Prometheus
    uid: prometheus
    type: prometheus
    access: proxy
    url: http://127.0.0.1:9090
    isDefault: true
DS
cat > /etc/grafana/provisioning/dashboards/oskarro.yml <<'DB'
apiVersion: 1
providers:
  - name: oskarro
    folder: Oskarro
    type: file
    allowUiUpdates: true
    options:
      path: /var/lib/grafana/dashboards
DB
# Community dashboards: Node Exporter Full (1860), NGINX exporter (12708), JVM Micrometer (4701).
for id in 1860 12708 4701; do
  wget -qO "/tmp/dash-$id.json" "https://grafana.com/api/dashboards/$id/revisions/latest/download"
  sed -e 's/\${DS_PROMETHEUS[^}]*}/prometheus/g' -e 's/\${DS_PROMETHEUS}/prometheus/g' "/tmp/dash-$id.json" > "/var/lib/grafana/dashboards/community-$id.json"
done
chown -R grafana:grafana /var/lib/grafana/dashboards

systemctl unmask grafana-server >/dev/null 2>&1 || true
systemctl enable -q grafana-server
systemctl restart grafana-server
for i in $(seq 1 30); do wget -qO /dev/null http://127.0.0.1:3000/api/health && break || sleep 2; done
grafana-cli --homepath /usr/share/grafana --config /etc/grafana/grafana.ini admin reset-admin-password "$GRAFANA_PASSWORD" >/dev/null 2>&1 \
  || printf '%s\n' "$GRAFANA_PASSWORD" | runuser -u grafana -- grafana cli --homepath /usr/share/grafana --config /etc/grafana/grafana.ini admin reset-admin-password --password-from-stdin >/dev/null

echo "== nginx: /grafana/"
F=/etc/nginx/sites-available/oskarro
if ! grep -q "location ^~ /grafana/" "$F"; then
  cp "$F" /root/backups/oskarro.nginx.before-grafana
  python3 - "$F" <<'PY'
import sys
p = sys.argv[1]; s = open(p).read()
block = """    location ^~ /grafana/ {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $http_connection;
    }

    location ^~ /status/ {"""
s = s.replace("    location ^~ /status/ {", block, 1)
open(p, "w").write(s)
PY
fi
if nginx -t 2>/tmp/ngt; then systemctl reload nginx; else cat /tmp/ngt; cp /root/backups/oskarro.nginx.before-grafana "$F"; systemctl reload nginx; exit 1; fi

echo "== check"
for t in 9090 9100 9113 3000; do ss -ltn "sport = :$t" | grep -q 127.0.0.1 && echo "127.0.0.1:$t ok" || echo "127.0.0.1:$t MISSING"; done
