#!/usr/bin/env bash
# Builds backend + frontend and deploys them to oskarro.com.
# Before replacing anything it saves the live jar and site to /root/backups/<timestamp>/,
# so a bad release can be undone with: scripts/deploy.sh --rollback <timestamp>
set -euo pipefail

HOST="${OSKARRO_SSH:-root@193.33.111.35}"
SSH=(ssh -o ControlPath=~/.ssh/cm-%C -o ControlMaster=auto -o ControlPersist=4h "$HOST")
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}"

# Remote snippet: replace /var/www/oskarro with /var/www/oskarro.next (keeps .well-known for certbot).
SWAP='swap_site() {
  [ -d /var/www/oskarro/.well-known ] && cp -a /var/www/oskarro/.well-known /var/www/oskarro.next/
  chown -R www-data:www-data /var/www/oskarro.next && chmod -R u+rwX,g+rwX,o+rX /var/www/oskarro.next
  rm -rf /var/www/oskarro.old && mv /var/www/oskarro /var/www/oskarro.old && mv /var/www/oskarro.next /var/www/oskarro && rm -rf /var/www/oskarro.old
}'

wait_for_api() {
  "${SSH[@]}" 'for i in $(seq 1 60); do
      if systemctl is-active -q oskarro && ss -ltn | grep -q "127.0.0.1\]*:8081"; then echo "API up after $((i*2))s"; exit 0; fi
      sleep 2
    done
    echo "API did not come up"; journalctl -u oskarro -n 60 --no-pager; exit 1'
}

if [[ "${1:-}" == "--rollback" ]]; then
  TS="${2:?usage: deploy.sh --rollback <timestamp>}"
  "${SSH[@]}" "$SWAP; set -e; B=/root/backups/$TS
    install -o oskarro -g oskarro -m 644 \$B/server.jar /opt/oskarro/server.jar
    rm -rf /var/www/oskarro.next && mkdir /var/www/oskarro.next && tar xzf \$B/web.tgz -C /var/www/oskarro.next
    swap_site
    systemctl restart oskarro"
  wait_for_api
  exit 0
fi

echo "== build backend"
(cd "$ROOT" && ./mvnw -q -pl server package)
echo "== build frontend"
(cd "$ROOT/frontend" && npx ng build --configuration production >/dev/null)

TS="$(date +%Y%m%d-%H%M%S)"
echo "== backup live release to /root/backups/$TS"
"${SSH[@]}" "set -e; mkdir -p /root/backups/$TS && chmod 700 /root/backups
  cp -p /opt/oskarro/server.jar /root/backups/$TS/server.jar
  tar czf /root/backups/$TS/web.tgz -C /var/www/oskarro --exclude .well-known ."

echo "== upload"
"${SSH[@]}" 'cat > /opt/oskarro/server.jar.new' < "$ROOT/server/target/server-0.0.1-SNAPSHOT.jar"
COPYFILE_DISABLE=1 tar --no-xattrs -czf - -C "$ROOT/frontend/dist/frontend/browser" . \
  | "${SSH[@]}" 'rm -rf /var/www/oskarro.next && mkdir /var/www/oskarro.next && tar xzf - -C /var/www/oskarro.next'

echo "== switch"
"${SSH[@]}" "$SWAP; set -e
  chown oskarro:oskarro /opt/oskarro/server.jar.new && mv /opt/oskarro/server.jar.new /opt/oskarro/server.jar
  swap_site
  systemctl restart oskarro"
wait_for_api
echo "== deployed $TS (rollback: scripts/deploy.sh --rollback $TS)"
