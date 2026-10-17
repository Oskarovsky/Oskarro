#!/usr/bin/env python3
"""oskarro.com health check, run every 5 minutes by oskarro-health.timer.

Checks the public site and API, the services behind them, disk, memory, the TLS
certificate and the last backup. Writes /var/www/status/{status.json,index.html}
(served at https://oskarro.com/status/ behind basic auth) and keeps 30 days of history.
If the API fails twice in a row it restarts the backend once and records that.
"""
import datetime as dt
import html
import json
import os
import shutil
import socket
import ssl
import subprocess
import time
import urllib.request

SITE = "https://oskarro.com"
OUT_DIR = "/var/www/status"
STATE = "/var/lib/oskarro-health/state.json"
HISTORY = "/var/lib/oskarro-health/history.jsonl"
BACKUPS = "/var/backups/oskarro"
KEEP_DAYS = 30


def http(path, timeout=15):
    started = time.monotonic()
    try:
        req = urllib.request.Request(SITE + path, headers={"User-Agent": "oskarro-health"})
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            resp.read(65536)
            return resp.status, int((time.monotonic() - started) * 1000)
    except urllib.error.HTTPError as err:
        return err.code, int((time.monotonic() - started) * 1000)
    except Exception:
        return 0, int((time.monotonic() - started) * 1000)


def unit_active(name):
    return subprocess.run(["systemctl", "is-active", "--quiet", name]).returncode == 0


def cert_days_left(host="oskarro.com"):
    ctx = ssl.create_default_context()
    with socket.create_connection((host, 443), timeout=10) as sock:
        with ctx.wrap_socket(sock, server_hostname=host) as tls:
            not_after = tls.getpeercert()["notAfter"]
    expires = dt.datetime.strptime(not_after, "%b %d %H:%M:%S %Y %Z")
    return (expires - dt.datetime.utcnow()).days


def memory_used_pct():
    info = {}
    with open("/proc/meminfo") as f:
        for line in f:
            key, value = line.split(":", 1)
            info[key] = int(value.split()[0])
    return round(100 * (1 - info["MemAvailable"] / info["MemTotal"]))


def backup_age_hours():
    dirs = [d for d in os.listdir(BACKUPS) if d[:1].isdigit()] if os.path.isdir(BACKUPS) else []
    if not dirs:
        return None
    newest = max(os.path.getmtime(os.path.join(BACKUPS, d)) for d in dirs)
    return round((time.time() - newest) / 3600, 1)


def check(name, ok, detail, warn=False):
    return {"name": name, "status": "ok" if ok else ("warn" if warn else "fail"), "detail": detail}


def run_checks():
    results = []
    code, ms = http("/")
    results.append(check("Strona główna", code == 200 and ms < 5000, f"HTTP {code}, {ms} ms"))
    api_code, api_ms = http("/api/stats")
    results.append(check("API (/api/stats)", api_code == 200 and api_ms < 5000, f"HTTP {api_code}, {api_ms} ms"))
    for unit, label in [("oskarro", "Backend (oskarro)"), ("nginx", "Nginx"),
                        ("postgresql@13-main", "PostgreSQL"), ("fail2ban", "fail2ban")]:
        active = unit_active(unit)
        results.append(check(label, active, "działa" if active else "NIE DZIAŁA"))
    firewall = subprocess.run(["ufw", "status"], capture_output=True, text=True).stdout.startswith("Status: active")
    results.append(check("Firewall (ufw)", firewall, "aktywny" if firewall else "WYŁĄCZONY"))
    disk = shutil.disk_usage("/")
    disk_pct = round(100 * disk.used / disk.total)
    results.append(check("Dysk", disk_pct < 85, f"{disk_pct}% zajęte, wolne {disk.free // 2**30} GB", warn=disk_pct < 95))
    mem = memory_used_pct()
    results.append(check("Pamięć RAM", mem < 90, f"{mem}% zajęte", warn=True))
    try:
        days = cert_days_left()
        results.append(check("Certyfikat SSL", days > 14, f"ważny jeszcze {days} dni", warn=days > 3))
    except Exception as exc:
        results.append(check("Certyfikat SSL", False, f"błąd: {exc.__class__.__name__}"))
    age = backup_age_hours()
    results.append(check("Kopia zapasowa", age is not None and age < 26,
                         "brak kopii" if age is None else f"ostatnia {age} h temu", warn=age is not None and age < 50))
    return results, api_code == 200


def load_json(path, default):
    try:
        with open(path) as f:
            return json.load(f)
    except Exception:
        return default


def main():
    os.makedirs(os.path.dirname(STATE), exist_ok=True)
    os.makedirs(OUT_DIR, exist_ok=True)
    now = dt.datetime.now().astimezone()
    results, api_ok = run_checks()

    state = load_json(STATE, {"api_failures": 0, "restarts": []})
    action = None
    if api_ok:
        state["api_failures"] = 0
    else:
        state["api_failures"] += 1
        if state["api_failures"] == 2:
            subprocess.run(["systemctl", "restart", "oskarro"])
            action = "Automatyczny restart backendu (API nie odpowiadało 2 razy z rzędu)"
            state["restarts"] = (state["restarts"] + [now.isoformat(timespec="seconds")])[-20:]
    with open(STATE, "w") as f:
        json.dump(state, f)

    overall = "fail" if any(r["status"] == "fail" for r in results) else (
        "warn" if any(r["status"] == "warn" for r in results) else "ok")
    entry = {"time": now.isoformat(timespec="seconds"), "overall": overall,
             "failed": [r["name"] for r in results if r["status"] == "fail"], "action": action}
    with open(HISTORY, "a") as f:
        f.write(json.dumps(entry) + "\n")
    cutoff = now - dt.timedelta(days=KEEP_DAYS)
    history = []
    with open(HISTORY) as f:
        for line in f:
            try:
                item = json.loads(line)
                if dt.datetime.fromisoformat(item["time"]) >= cutoff:
                    history.append(item)
            except Exception:
                pass
    with open(HISTORY, "w") as f:
        f.writelines(json.dumps(item) + "\n" for item in history)

    def uptime(hours):
        since = now - dt.timedelta(hours=hours)
        window = [h for h in history if dt.datetime.fromisoformat(h["time"]) >= since]
        if not window:
            return None
        return round(100 * sum(1 for h in window if h["overall"] != "fail") / len(window), 2)

    summary = {
        "checked_at": now.isoformat(timespec="seconds"),
        "overall": overall,
        "checks": results,
        "uptime": {"24h": uptime(24), "7d": uptime(24 * 7), "30d": uptime(24 * 30)},
        "restarts": state["restarts"],
        "incidents": [h for h in history if h["overall"] == "fail" or h.get("action")][-30:],
    }
    tmp = os.path.join(OUT_DIR, ".status.json")
    with open(tmp, "w") as f:
        json.dump(summary, f, ensure_ascii=False, indent=1)
    os.replace(tmp, os.path.join(OUT_DIR, "status.json"))
    tmp = os.path.join(OUT_DIR, ".index.html")
    with open(tmp, "w") as f:
        f.write(render(summary))
    os.replace(tmp, os.path.join(OUT_DIR, "index.html"))
    if overall == "fail":
        subprocess.run(["logger", "-t", "oskarro-health", "FAIL: " + ", ".join(entry["failed"])])


LABEL = {"ok": "Działa", "warn": "Uwaga", "fail": "Awaria"}


def render(s):
    rows = "".join(
        f'<tr class="{c["status"]}"><td><span class="dot"></span>{html.escape(c["name"])}</td>'
        f'<td>{html.escape(c["detail"])}</td><td>{LABEL[c["status"]]}</td></tr>' for c in s["checks"])
    incidents = "".join(
        f'<li><time>{html.escape(i["time"][:16].replace("T", " "))}</time> '
        f'{html.escape(", ".join(i["failed"]) or "—")}'
        f'{(" — " + html.escape(i["action"])) if i.get("action") else ""}</li>'
        for i in reversed(s["incidents"])) or "<li>Brak awarii w ostatnich 30 dniach.</li>"
    up = s["uptime"]
    fmt = lambda v: "—" if v is None else f"{v}%"
    return f"""<!doctype html><html lang="pl"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><meta http-equiv="refresh" content="60">
<meta name="robots" content="noindex"><title>Status oskarro.com</title>
<style>
:root{{--bg:#12110e;--card:#221f19;--line:rgba(244,236,214,.14);--text:#f6f1e6;--muted:#b3a790;--ok:#7bd88f;--warn:#f2c14e;--fail:#ff6b5b}}
body{{margin:0;background:var(--bg);color:var(--text);font:15px/1.5 system-ui,-apple-system,Segoe UI,sans-serif}}
main{{max-width:860px;margin:0 auto;padding:28px 16px 60px}}
h1{{margin:0 0 4px;font-size:1.6rem}} .muted{{color:var(--muted)}}
.banner{{margin:18px 0;padding:16px 18px;border-radius:14px;background:var(--card);border:1px solid var(--line);font-weight:650;font-size:1.1rem}}
.banner.ok{{border-color:var(--ok)}} .banner.warn{{border-color:var(--warn)}} .banner.fail{{border-color:var(--fail)}}
.up{{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;margin-bottom:18px}}
.up div{{background:var(--card);border:1px solid var(--line);border-radius:12px;padding:12px}} .up strong{{display:block;font-size:1.4rem}}
.wrap{{overflow-x:auto}} table{{width:100%;border-collapse:collapse;background:var(--card);border-radius:12px;overflow:hidden}}
td{{padding:10px 12px;border-top:1px solid var(--line);white-space:nowrap}} tr:first-child td{{border-top:0}}
.dot{{display:inline-block;width:9px;height:9px;border-radius:50%;margin-right:8px;background:var(--ok)}}
tr.warn .dot{{background:var(--warn)}} tr.fail .dot{{background:var(--fail)}} tr.fail td:last-child{{color:var(--fail);font-weight:650}}
ul{{padding-left:18px}} time{{color:var(--muted);font-variant-numeric:tabular-nums}}
</style></head><body><main>
<h1>Status oskarro.com</h1>
<p class="muted">Sprawdzane co 5 minut · ostatnio {html.escape(s["checked_at"][:19].replace("T", " "))} · strona odświeża się sama</p>
<div class="banner {s["overall"]}">{ {"ok": "✅ Wszystko działa", "warn": "⚠️ Działa, ale coś wymaga uwagi", "fail": "❌ Awaria — sprawdź listę poniżej"}[s["overall"]] }</div>
<div class="up"><div><span class="muted">Dostępność 24 h</span><strong>{fmt(up["24h"])}</strong></div>
<div><span class="muted">7 dni</span><strong>{fmt(up["7d"])}</strong></div>
<div><span class="muted">30 dni</span><strong>{fmt(up["30d"])}</strong></div></div>
<div class="wrap"><table>{rows}</table></div>
<h2>Awarie i automatyczne naprawy</h2><ul>{incidents}</ul>
<p class="muted">Dane w formacie JSON: <a style="color:var(--text)" href="status.json">status.json</a></p>
</main></body></html>"""


if __name__ == "__main__":
    main()
