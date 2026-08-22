#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "usage: deploy-release.sh <release-bucket> <release-key> <40-char-revision>" >&2
  exit 2
fi

release_bucket="$1"
release_key="$2"
revision="$3"
release_root="/opt/moca/releases"
release_dir="${release_root}/${revision}"
current_link="/opt/moca/current"
previous_target=""
archive_path="$(mktemp /tmp/moca-release.XXXXXX.tgz)"

cleanup() {
  rm -f "${archive_path}"
}
trap cleanup EXIT

if [[ ! "${revision}" =~ ^[0-9a-f]{40}$ ]]; then
  echo "revision must be a 40-character lowercase Git SHA" >&2
  exit 2
fi

if [[ -L "${current_link}" ]]; then
  previous_target="$(readlink -f "${current_link}")"
fi

mkdir -p "${release_root}" "${release_dir}"
aws s3 cp "s3://${release_bucket}/${release_key}" "${archive_path}" --only-show-errors
tar -xzf "${archive_path}" -C "${release_dir}"

if [[ ! -f "${release_dir}/backend/app.jar" || ! -f "${release_dir}/frontend/dist/index.html" ]]; then
  echo "release archive is incomplete" >&2
  exit 1
fi

install -d -m 0755 /opt/moca/shared/db-init
install -m 0644 "${release_dir}/backend/src/main/resources/db/init/001-schema.sql" /opt/moca/shared/db-init/001-schema.sql
install -m 0644 "${release_dir}/backend/src/main/resources/db/init/002-seed.sql" /opt/moca/shared/db-init/002-seed.sql

docker compose \
  --env-file /etc/moca/app.env \
  -f "${release_dir}/deploy/compose.yaml" \
  up -d --wait --wait-timeout 120 mysql

ln -sfn "${release_dir}" "${current_link}.next"
mv -Tf "${current_link}.next" "${current_link}"
systemctl restart moca-backend.service

if curl --fail --silent --show-error --retry 20 --retry-delay 3 --retry-connrefused \
  http://127.0.0.1:8080/actuator/health >/dev/null; then
  install -m 0755 "${release_dir}/deploy/scripts/deploy-release.sh" /usr/local/bin/moca-deploy
  echo "deployed ${revision}"
  exit 0
fi

if [[ -n "${previous_target}" && -d "${previous_target}" ]]; then
  ln -sfn "${previous_target}" "${current_link}.next"
  mv -Tf "${current_link}.next" "${current_link}"
  systemctl restart moca-backend.service
fi

echo "health check failed; restored previous application release when available" >&2
exit 1
