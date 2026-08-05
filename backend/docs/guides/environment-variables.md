# 환경변수

## 관리 원칙

- 예시와 기본값은 `backend/.env.example`에 기록하고 Git에 커밋한다.
- 개인 값은 `backend/.env`에 기록하며 Git에 커밋하지 않는다.
- 실제 비밀번호, 토큰과 운영 비밀값은 문서, 코드, 예시 파일에 기록하지 않는다.
- 환경변수를 추가하거나 이름을 변경하면 애플리케이션 설정, Compose, `.env.example`과 이 문서를 같은 PR에서 수정한다.

## 현재 환경변수

| 환경변수 | 로컬 기본값 | 용도 |
| --- | --- | --- |
| `DB_HOST` | `localhost` | MySQL 호스트 |
| `DB_PORT` | `3306` | MySQL 포트 |
| `DB_NAME` | `jachwi_sunbae` | 데이터베이스 이름 |
| `DB_USERNAME` | `jachwi_sunbae` | 애플리케이션 계정 |
| `DB_PASSWORD` | `local_password` | 애플리케이션 계정 비밀번호 |
| `DB_ROOT_PASSWORD` | `local_root_password` | 로컬 MySQL root 비밀번호 |

## 사용 방법

`backend`에서 개인 파일을 생성한다.

```bash
cp .env.example .env
```

Docker Compose는 같은 디렉터리의 `.env`를 자동으로 읽는다. Spring Boot 프로세스는 `.env`를 자동으로 읽지 않으므로 기본값이 아닌 값을 사용할 때는 실행 전에 환경변수를 전달한다.

```bash
set -a
source .env
set +a
./gradlew bootRun
```

운영 환경의 비밀 관리 방식은 배포 환경을 선택할 때 별도로 결정한다.
