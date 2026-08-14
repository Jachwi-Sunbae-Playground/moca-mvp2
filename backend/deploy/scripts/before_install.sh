#!/usr/bin/env bash
set -euo pipefail

APP_DIR=/opt/jachwi-sunbae

# CodeDeploy는 대상 경로에 이 배포가 만들지 않은 파일이 있으면 실패한다.
# 운영 환경변수는 /etc/jachwi-sunbae/app.env 에 있어 여기서 지워지지 않는다.
mkdir -p "${APP_DIR}"
rm -rf "${APP_DIR:?}"/*

echo "${APP_DIR} 를 비웠다."
