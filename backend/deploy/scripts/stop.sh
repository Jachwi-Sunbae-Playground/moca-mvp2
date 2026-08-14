#!/usr/bin/env bash
set -euo pipefail

SERVICE=jachwi-sunbae.service

# 서비스가 없어도 실패하지 않게 둔다. 존재 여부를 미리 검사하지 않는 이유는
# `systemctl list-unit-files` 출력 형식이 환경에 따라 달라 검사가 어긋날 수 있기 때문이다.
# 실제로 그렇게 어긋나 중지가 건너뛰어진 적이 있다.
#
# 실제 교체는 ApplicationStart 의 `systemctl restart` 가 보장한다. 이 훅은 배포 중 트래픽을
# 먼저 끊는 역할만 한다.
systemctl stop "${SERVICE}" 2>/dev/null || true
echo "${SERVICE} 중지를 요청했다."
