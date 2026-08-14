#!/usr/bin/env bash
set -euo pipefail

SERVICE=jachwi-sunbae.service

# start 가 아니라 restart 다. 서비스가 이미 active 이면 start 는 아무 일도 하지 않고,
# 옛 프로세스가 그대로 남은 채 배포가 끝난다. 그 상태에서 health 를 확인하면 옛 프로세스가
# 응답해 배포가 성공으로 기록된다. 실제로는 아무것도 바뀌지 않은 배포다.
#
# ApplicationStop 이 중지해줄 것이라고 가정하지 않는다. 그 훅은 직전 리비전의 스크립트로
# 실행되므로 첫 배포에서는 아예 실행되지 않고, 직전 리비전의 스크립트가 잘못됐으면 동작하지도 않는다.
systemctl restart "${SERVICE}"
echo "${SERVICE} 를 재시작했다. 기동 확인은 ValidateService 에서 한다."
