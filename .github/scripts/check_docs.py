#!/usr/bin/env python3
"""문서 정합성을 검사한다.

파생 문서가 정본과 어긋나면 실패한다. 검사 대상과 정본은
docs/convention/documentation.md에 기록한다.

로컬 실행: python3 .github/scripts/check_docs.py
"""

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

LINK = re.compile(r"!?\[[^\]]*\]\(([^)]+)\)")
EXTERNAL = ("http://", "https://", "mailto:", "#")

failures = []


def fail(check, message):
    failures.append((check, message))


def tracked(pattern):
    out = subprocess.run(
        ["git", "ls-files", pattern], cwd=ROOT, capture_output=True, text=True, check=True
    )
    return [ROOT / line for line in out.stdout.split()]


def read(path):
    return (ROOT / path).read_text(encoding="utf-8")


def rel(path):
    return str(Path(path).relative_to(ROOT))


def fenced_block(text, heading, language="markdown"):
    """`heading` 다음에 처음 나오는 코드 펜스 내용을 돌려준다."""
    start = text.index(heading)
    pattern = re.compile(r"```" + language + r"\n(.*?)\n```", re.S)
    match = pattern.search(text, start)
    return match.group(1) if match else None


def normalize(text):
    return [line.rstrip() for line in text.strip().splitlines()]


# --- A-1. 상대 링크가 실제 파일을 가리키는가 -----------------------------------


def check_links():
    for path in tracked("*.md"):
        for number, line in enumerate(read(rel(path)).splitlines(), 1):
            for target in LINK.findall(line):
                target = target.strip()
                if target.startswith(EXTERNAL):
                    continue
                target = target.split("#")[0]
                if not target:
                    continue
                resolved = (path.parent / target).resolve()
                if not resolved.exists():
                    fail("A-1 링크", f"{rel(path)}:{number} 의 링크 대상이 없다: {target}")


# --- A-2. 이슈·PR 템플릿이 컨벤션 문서와 같은가 --------------------------------


def check_templates():
    doc = read("docs/convention/issue-and-pr.md")

    issue_doc = fenced_block(doc, "## 이슈")
    issue_file = re.sub(r"^---\n.*?\n---\n\n", "", read(".github/ISSUE_TEMPLATE/issue.md"), flags=re.S)
    if normalize(issue_doc) != normalize(issue_file):
        fail(
            "A-2 템플릿",
            ".github/ISSUE_TEMPLATE/issue.md 가 docs/convention/issue-and-pr.md 의 이슈 본문과 다르다",
        )

    pr_doc = fenced_block(doc, "## PR")
    pr_file = read(".github/pull_request_template.md")
    if normalize(pr_doc) != normalize(pr_file):
        fail(
            "A-2 템플릿",
            ".github/pull_request_template.md 가 docs/convention/issue-and-pr.md 의 PR 본문과 다르다",
        )


# --- B-1. 목차 문서가 실제 파일 목록과 같은가 ---------------------------------


def check_convention_index():
    doc = read("docs/convention/README.md")
    listed = set(re.findall(r"\]\(([a-z0-9-]+\.md)\)", doc))
    actual = {p.name for p in (ROOT / "docs/convention").glob("*.md")} - {"README.md"}
    for name in sorted(actual - listed):
        fail("B-1 목차", f"docs/convention/README.md 에 {name} 이 없다")
    for name in sorted(listed - actual):
        fail("B-1 목차", f"docs/convention/README.md 가 없는 파일을 가리킨다: {name}")


def check_adr_index():
    doc = read("backend/docs/adr/README.md")
    listed = set(re.findall(r"\]\((\d{4}-[a-z0-9-]+\.md)\)", doc))
    actual = {p.name for p in (ROOT / "backend/docs/adr").glob("[0-9]*.md")}
    for name in sorted(actual - listed):
        fail("B-1 목차", f"backend/docs/adr/README.md 의 표에 {name} 이 없다")
    for name in sorted(listed - actual):
        fail("B-1 목차", f"backend/docs/adr/README.md 가 없는 ADR을 가리킨다: {name}")


def check_backend_docs_index():
    doc = read("backend/docs/README.md")
    listed = set(re.findall(r"\| \[`([a-z]+)`\]", doc))
    actual = {p.name for p in (ROOT / "backend/docs").iterdir() if p.is_dir()}
    for name in sorted(actual - listed):
        fail("B-1 목차", f"backend/docs/README.md 의 표에 {name} 디렉터리가 없다")
    for name in sorted(listed - actual):
        fail("B-1 목차", f"backend/docs/README.md 가 없는 디렉터리를 가리킨다: {name}")


def check_root_readme():
    doc = read("README.md")

    top_level = {
        p.name
        for p in ROOT.iterdir()
        if p.name not in {".git", ".idea", ".claude"} and not p.name.startswith(".DS")
    }
    tracked_top = {rel(p).split("/")[0] for p in tracked("*")}
    for name in sorted(top_level & tracked_top):
        if name not in doc:
            fail("B-1 목차", f"README.md 의 저장소 구조에 {name} 이 없다")

    for path in sorted((ROOT / "docs").iterdir()):
        if path.is_dir() and f"docs/{path.name}" not in doc:
            fail("B-1 목차", f"README.md 에 docs/{path.name} 에 대한 안내가 없다")


# --- B-2. 환경변수 표가 .env.example 과 같은가 --------------------------------


def check_env_vars():
    doc = read("backend/docs/guides/environment-variables.md")
    listed = set(re.findall(r"^\| `([A-Z][A-Z0-9_]*)`", doc, re.M))
    actual = set(re.findall(r"^([A-Z][A-Z0-9_]*)=", read("backend/.env.example"), re.M))
    for name in sorted(actual - listed):
        fail("B-2 환경변수", f"environment-variables.md 의 표에 {name} 이 없다")
    for name in sorted(listed - actual):
        fail("B-2 환경변수", f"environment-variables.md 가 .env.example 에 없는 {name} 을 설명한다")


CHECKS = [
    check_links,
    check_templates,
    check_convention_index,
    check_adr_index,
    check_backend_docs_index,
    check_root_readme,
    check_env_vars,
]


def main():
    for check in CHECKS:
        check()

    if not failures:
        print("문서 정합성 검사를 통과했다.")
        return 0

    print(f"문서 정합성 검사에서 {len(failures)}건을 발견했다.\n")
    for name, message in failures:
        print(f"  [{name}] {message}")
    print("\n정본과 대조 대상은 docs/convention/documentation.md 를 참고한다.")
    return 1


if __name__ == "__main__":
    sys.exit(main())
