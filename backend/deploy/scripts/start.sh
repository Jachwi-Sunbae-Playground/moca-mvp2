#!/usr/bin/env bash
set -euo pipefail

SERVICE=jachwi-sunbae.service

systemctl start "${SERVICE}"
echo "${SERVICE} 를 시작했다. 기동 확인은 ValidateService 에서 한다."
