#!/usr/bin/env bash
set -euo pipefail

APP_DIR=/opt/jachwi-sunbae
ENV_FILE=/etc/jachwi-sunbae/app.env
SERVICE=jachwi-sunbae.service
RUN_USER=jachwi

# 환경변수 파일은 배포 산출물에 넣지 않는다. 사람이 서버에 한 번 만들어 둔 것을 쓴다.
# 없으면 애플리케이션이 기동 도중에 죽으므로 여기서 먼저 멈춘다.
if [[ ! -f "${ENV_FILE}" ]]; then
    echo "운영 환경변수 파일이 없다: ${ENV_FILE}" >&2
    exit 1
fi

if ! id "${RUN_USER}" &>/dev/null; then
    echo "실행 사용자가 없다: ${RUN_USER}" >&2
    exit 1
fi

chown -R "${RUN_USER}:${RUN_USER}" "${APP_DIR}"
chmod 0640 "${APP_DIR}/app.jar"
chmod 0755 "${APP_DIR}"/scripts/*.sh

install -m 0644 "${APP_DIR}/${SERVICE}" "/etc/systemd/system/${SERVICE}"
systemctl daemon-reload
systemctl enable "${SERVICE}"

echo "${SERVICE} 를 설치했다."
