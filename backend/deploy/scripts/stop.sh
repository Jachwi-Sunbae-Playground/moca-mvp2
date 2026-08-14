#!/usr/bin/env bash
set -euo pipefail

SERVICE=jachwi-sunbae.service

# 첫 배포에는 이 훅이 실행되지 않지만, 서비스가 없는 상태에서 실행돼도 실패하지 않게 둔다.
if systemctl list-unit-files --no-legend | grep -q "^${SERVICE}"; then
    systemctl stop "${SERVICE}"
    echo "${SERVICE} 를 중지했다."
else
    echo "${SERVICE} 가 없어 중지할 것이 없다."
fi
