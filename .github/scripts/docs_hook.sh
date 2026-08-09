#!/usr/bin/env bash
# 문서를 수정한 직후 정합성을 검사한다.
#
# Claude Code(.claude/settings.json)와 Codex(.codex/hooks.json)의 PostToolUse 훅이
# 이 스크립트를 함께 사용한다. 훅 설정은 도구마다 형식이 다르지만 판단 로직은
# 이 파일 한 곳에만 둔다.
#
# 훅 이벤트 JSON을 stdin으로 받는다. 수정된 파일 경로를 알아내지 못하면
# 건너뛰지 않고 검사한다.

set -u

payload=$(cat)

root=$(git rev-parse --show-toplevel 2>/dev/null) || exit 0

path=$(printf '%s' "$payload" | jq -r '
  .tool_input.file_path
  // .tool_input.path
  // .tool_response.filePath
  // empty
' 2>/dev/null) || path=""

case "$path" in
  "") ;;
  *.md | *.env.example | *ISSUE_TEMPLATE* | *pull_request_template*) ;;
  *) exit 0 ;;
esac

if ! out=$(python3 "$root/.github/scripts/check_docs.py" 2>&1); then
  printf '%s\n' "$out" >&2
  exit 2
fi

exit 0
