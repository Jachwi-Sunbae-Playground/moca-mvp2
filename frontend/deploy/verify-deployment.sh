#!/usr/bin/env bash
set -euo pipefail

LOCAL_INDEX="${1:?로컬 index.html 경로가 필요하다.}"
DEPLOYED_INDEX_URL="${2:?배포된 index.html URL이 필요하다.}"

if [[ ! -f "${LOCAL_INDEX}" ]]; then
    echo "로컬 index.html이 없다: ${LOCAL_INDEX}" >&2
    exit 1
fi

EXPECTED_ASSETS="$({
    grep -oE '(src|href)="[^"]+\.(js|css)(\?[^"]*)?"' "${LOCAL_INDEX}" \
        | sed -E 's/^[^=]+="//; s/"$//' \
        | sort -u
} || true)"

if [[ -z "${EXPECTED_ASSETS}" ]]; then
    echo "로컬 index.html에서 JS 또는 CSS 파일명을 찾지 못했다: ${LOCAL_INDEX}" >&2
    exit 1
fi

DEPLOYED_INDEX="$(curl \
    -fsS \
    --retry 12 \
    --retry-all-errors \
    --retry-delay 5 \
    --max-time 10 \
    -H 'Cache-Control: no-cache' \
    "${DEPLOYED_INDEX_URL}")"

while IFS= read -r asset; do
    if ! grep -Fq "${asset}" <<< "${DEPLOYED_INDEX}"; then
        echo "배포된 index.html이 이번 빌드 파일을 참조하지 않는다: ${asset}" >&2
        exit 1
    fi
done <<< "${EXPECTED_ASSETS}"

echo "배포된 index.html에서 이번 빌드 파일을 확인했다:"
printf '%s\n' "${EXPECTED_ASSETS}"
