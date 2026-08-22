# MVP2 백엔드 배포

- 상태: 저장소 기반 준비 완료, 미배포
- 문서 성격: 파생
- 대조 대상: `deploy/`, `.github/workflows/deploy-production.yml`, 백엔드 운영 설정

전체 구성과 AWS 준비 조건은 [MVP2 배포 아키텍처](../../../docs/operations/mvp2-deployment-architecture.md)를 따른다. 이 문서는 Spring Boot와 MySQL에 한정된 실행 기준만 다룬다.

## 실행 경계

- Caddy가 외부 80·443을 받고 `/api/*`를 Spring Boot `127.0.0.1:8080`으로 전달한다.
- Spring Boot는 `moca-backend.service`, `prod` 프로필과 `moca` 비로그인 계정으로 실행한다.
- 운영 환경변수는 `/etc/moca/app.env`에서 주입한다.
- MySQL은 `deploy/compose.yaml`로 실행하고 `127.0.0.1:3306`에만 바인딩한다.
- 사진 객체는 구현 완료 후 EC2 instance role로 비공개 S3에 접근한다. 정적 Access Key를 두지 않는다.

## 최초 준비

첫 배포 Issue에서 다음 순서로 한 번만 준비한다.

1. Java 21, Docker와 AWS CLI를 설치한다.
2. `moca` 계정과 `/opt/moca/releases`, `/etc/moca`를 만든다.
3. `deploy/moca-backend.service`를 `/etc/systemd/system/`에 설치하고 활성화한다.
4. `deploy/scripts/deploy-release.sh`를 `/usr/local/bin/moca-deploy`에 `0755`로 설치한다.
5. `/etc/moca/app.env`를 `root:root`, `0600`으로 작성한다.
6. Docker 서비스가 실행 중인지 확인한다. 첫 수동 배포가 `deploy/compose.yaml`을 실행해 빈 MySQL을 초기화한다.

DB 초기화 기준은 [데이터베이스 초기화](../guides/database-initialization.md)를 따른다. 데이터가 생긴 뒤 `docker compose down -v`를 실행하지 않는다.

## 필수 운영 환경변수

로컬 변수의 의미는 [환경변수](../guides/environment-variables.md)에 있다. 운영에서는 추가로 다음 값을 확인한다.

| 항목 | 운영 기준 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | systemd에서 `prod`로 고정 |
| `DB_HOST` | `127.0.0.1` |
| `DB_PORT` | `3306` |
| `DB_SSL_MODE` | MySQL 서버 TLS 구성에 맞추되 기본 `REQUIRED` |
| `CORS_ALLOWED_ORIGINS` | 공개 프론트 Origin 하나 |
| `PHOTO_STORAGE_ENDPOINT` | AWS S3에서는 설정하지 않음 |
| `PHOTO_STORAGE_REGION` | 사진 버킷 리전 |
| `PHOTO_STORAGE_BUCKET` | 비공개 사진 버킷 |
| `PHOTO_STORAGE_ACCESS_KEY`, `PHOTO_STORAGE_SECRET_KEY` | AWS에서는 설정하지 않음 |

## 배포 결과 확인

배포 스크립트가 내부 health를 확인한다. 수동 점검이 필요할 때 EC2에서 다음을 확인한다.

```bash
systemctl status moca-backend.service
journalctl -u moca-backend.service -n 200 --no-pager
curl --fail http://127.0.0.1:8080/actuator/health
curl --fail http://127.0.0.1:8080/actuator/info
```

공개 도메인에서는 필요한 `/api`만 노출한다. Actuator와 Swagger를 Caddy의 공개 경로에 추가하지 않는다.
