#!/usr/bin/env bash
set -euo pipefail

SERVICE=jachwi-sunbae.service
HEALTH_URL=http://127.0.0.1:8080/actuator/health
ATTEMPTS=48
INTERVAL=5

# Flyway 마이그레이션이 포함된 첫 기동은 시간이 걸린다. 넉넉히 기다리되 무한정 기다리지는 않는다.
for attempt in $(seq 1 "${ATTEMPTS}"); do
    if ! systemctl is-active --quiet "${SERVICE}"; then
        echo "${SERVICE} 가 실행 중이 아니다. 기동에 실패했다." >&2
        journalctl -u "${SERVICE}" -n 200 --no-pager >&2 || true
        exit 1
    fi

    if curl -fsS --max-time 5 "${HEALTH_URL}" 2>/dev/null | grep -q '"status":"UP"'; then
        echo "health 확인됨 (${attempt}번째 시도)."
        exit 0
    fi

    sleep "${INTERVAL}"
done

echo "health 가 $((ATTEMPTS * INTERVAL))초 안에 UP 이 되지 않았다." >&2
journalctl -u "${SERVICE}" -n 200 --no-pager >&2 || true
exit 1
