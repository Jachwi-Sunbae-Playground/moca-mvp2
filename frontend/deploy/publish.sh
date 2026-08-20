#!/usr/bin/env bash
set -euo pipefail

DEPLOY_ENVIRONMENT="${1:?배포 환경은 dev 또는 prod여야 한다.}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIST_DIR="${SCRIPT_DIR}/../dist"
LOCAL_INDEX="${DIST_DIR}/index.html"

case "${DEPLOY_ENVIRONMENT}" in
    dev)
        S3_URI=s3://techcourse-project-2026/jachwi-sunbae/web-dev/
        DISTRIBUTION_ID=ETE1HH7V9K0PO
        DEPLOYED_INDEX_URL=https://dev.jachwi-sunbae.kr/index.html
        ;;
    prod)
        S3_URI=s3://techcourse-project-2026/jachwi-sunbae/web/
        DISTRIBUTION_ID=E3LI41UZ24V9WD
        DEPLOYED_INDEX_URL=https://www.jachwi-sunbae.kr/index.html
        ;;
    *)
        echo "알 수 없는 배포 환경이다: ${DEPLOY_ENVIRONMENT}" >&2
        exit 1
        ;;
esac

if [[ ! -f "${LOCAL_INDEX}" ]]; then
    echo "프론트엔드 빌드 결과가 없다: ${LOCAL_INDEX}" >&2
    exit 1
fi

export AWS_PAGER=""

aws s3 sync "${DIST_DIR}/" "${S3_URI}" --delete

INVALIDATION_ID="$(aws cloudfront create-invalidation \
    --distribution-id "${DISTRIBUTION_ID}" \
    --paths '/index.html' \
    --query 'Invalidation.Id' \
    --output text)"

if [[ -z "${INVALIDATION_ID}" || "${INVALIDATION_ID}" == "None" ]]; then
    echo "CloudFront 무효화 ID를 받지 못했다." >&2
    exit 1
fi

aws cloudfront wait invalidation-completed \
    --distribution-id "${DISTRIBUTION_ID}" \
    --id "${INVALIDATION_ID}"

"${SCRIPT_DIR}/verify-deployment.sh" "${LOCAL_INDEX}" "${DEPLOYED_INDEX_URL}"
