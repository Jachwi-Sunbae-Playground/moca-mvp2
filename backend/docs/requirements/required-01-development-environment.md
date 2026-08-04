# 지정 요구사항 1: 개발 환경을 구축하고 팀이 소유한다

- 상태: 진행 중
- 최초 기록일: 2026-08-04
- 현재 판단: 초기 구성자와 최초 PR CI 검증 완료, 다른 팀원의 독립 재현과 팀 승인 필요

## 1. 목적

팀원이 같은 기준으로 제품을 개발, 빌드, 테스트, 실행하고 변경 사항을 검증할 수 있는 환경을 만든다. 환경이 특정 팀원의 기억이나 이미 설정된 로컬 상태에만 의존하지 않게 한다.

## 2. 제출 내용과 증거

| 제출 내용 | 기록 위치 |
| --- | --- |
| 개발 환경 구성 및 실행 방법 | [백엔드 로컬 개발 환경 가이드](../development/backend-local-setup.md) |
| 주요 기술 선택과 판단 근거 | [ADR 목록](../adr/README.md) |
| 검토한 대안과 트레이드오프 | 각 ADR의 `검토한 대안`, `결과와 트레이드오프` |
| 환경 재현 또는 실행 확인 결과 | 이 문서의 검증 결과와 팀원 재현 기록 |

## 3. 결정 요약과 기록 근거

| 결정 | 선택한 이유 | 상세 기록 |
| --- | --- | --- |
| 모노레포 | 코드, 문서, CI 변경을 한 PR에서 검토하고 저장소 관리 비용을 줄인다 | [ADR-0001](../adr/0001-use-monorepo.md) |
| Java 21 | 장기 지원 버전이며 팀 경험과 안정성의 균형이 좋다 | [ADR-0002](../adr/0002-backend-runtime-and-build-tools.md) |
| Spring Boot 3.5.16 | 4.1보다 팀 경험과 생태계 호환성 위험이 낮아 MVP에 집중할 수 있다 | [ADR-0002](../adr/0002-backend-runtime-and-build-tools.md) |
| Gradle 8.14.3 Wrapper | 별도 Gradle 설치 없이 팀 버전을 고정한다 | [ADR-0002](../adr/0002-backend-runtime-and-build-tools.md) |
| Lombok 미사용 | 생성 코드보다 명시적인 Java 코드와 공통 이해를 우선한다 | [ADR-0002](../adr/0002-backend-runtime-and-build-tools.md) |
| JDBC Template과 레이어드 구조 | 현재 MVP 복잡도에 맞게 구조 비용을 줄이고 SQL 동작을 명확히 본다 | [ADR-0003](../adr/0003-use-layered-architecture-and-jdbc-template-for-mvp.md) |
| JPA와 헥사고날 도입 보류 | 실제 모델링 복잡성이 확인된 뒤 비용과 이점을 다시 판단한다 | [ADR-0003](../adr/0003-use-layered-architecture-and-jdbc-template-for-mvp.md) |
| Docker Compose MySQL | 팀원의 로컬 설치 상태와 무관하게 같은 DB 버전을 실행한다 | [ADR-0004](../adr/0004-use-mysql-and-testcontainers.md) |
| Testcontainers MySQL | H2나 Mock이 놓치는 실제 SQL과 매핑을 검증한다 | [ADR-0004](../adr/0004-use-mysql-and-testcontainers.md) |
| 우테코 스타일과 EditorConfig | 익숙한 규칙을 재사용해 포맷 차이를 줄인다 | [ADR-0005](../adr/0005-use-wooteco-style-and-github-actions.md) |
| GitHub Actions CI | 별도 서버 없이 PR에서 동일한 빌드와 테스트를 검증한다 | [ADR-0005](../adr/0005-use-wooteco-style-and-github-actions.md) |

## 4. 구현 결과

| 항목 | 결과 | 증거 |
| --- | --- | --- |
| 모노레포 기본 구조 | 완료 | `backend`, `backend/docs`, `config`, `.github/workflows` |
| 버전이 고정된 백엔드 빌드 | 완료 | Gradle Wrapper, Java Toolchain, `backend/build.gradle` |
| 로컬 MySQL 재현 | 완료 | `compose.yaml`, `.env.example` |
| 실제 MySQL 통합 테스트 | 완료 | `IntegrationTest`, Testcontainers, `@ServiceConnection` |
| 공통 코드 스타일 | 완료 | `.editorconfig`, 우테코 IntelliJ XML |
| 로컬 실행 문서 | 완료 | [백엔드 로컬 개발 환경 가이드](../development/backend-local-setup.md) |
| PR 자동 검증 | 완료 | `.github/workflows/backend-ci.yml`, [PR #1 CI](https://github.com/woowacourse-teams/2026-jachwi-sunbae/actions/runs/30888179911) |

## 5. 최초 구성 환경 검증 결과

- 검증일: 2026-08-04
- 검증자: `softmoca`
- 운영체제: macOS
- Java: Temurin 21.0.4
- Docker: Docker Desktop 29.4.3
- Docker Compose: 5.1.4

| 검증 항목 | 결과 | 관찰한 사실 |
| --- | --- | --- |
| Gradle Wrapper | 성공 | Gradle 8.14.3과 Java 21 인식 |
| 로컬 MySQL 실행 | 성공 | MySQL 8.4.10 컨테이너가 `healthy`로 전환 |
| 애플리케이션 실행 | 성공 | Spring Boot 3.5.16이 `local` 기본 프로필로 실행 |
| DB 연결 상태 | 성공 | Actuator health가 `{"status":"UP"}` 응답, Hikari가 실제 MySQL 연결 생성 |
| OpenAPI | 성공 | `/v3/api-docs` 응답 확인 |
| 통합 테스트 | 성공 | Testcontainers MySQL에서 `SELECT 1` 실행 |
| 전체 빌드 | 성공 | `./gradlew clean build --no-daemon` 성공 |
| GitHub Actions | 성공 | PR #1의 Backend CI에서 Java 21 설정, Gradle 전체 빌드, Testcontainers 테스트 성공 |

## 6. 발생한 문제와 해결 방법

| 문제 또는 위험 | 확인한 내용 | 해결 또는 현재 대응 |
| --- | --- | --- |
| Spring Initializr 기본값이 Spring Boot 4.1 | 최신 메이저 도입 이점보다 팀 학습과 호환성 위험이 컸다 | Spring Boot 3.5.16과 호환 Gradle 버전을 명시적으로 고정했다 |
| 팀원별 MySQL 설치와 설정 차이 | JDBC SQL은 DB 제품과 버전 차이의 영향을 받는다 | MySQL 8.4.10 Docker 이미지를 Compose와 Testcontainers에서 함께 사용한다 |
| H2나 Mock으로 실제 SQL을 보장할 수 없음 | 호출 성공과 MySQL 쿼리 성공은 다른 문제다 | 실제 MySQL에서 `SELECT 1`을 실행하는 통합 테스트를 추가했다 |
| Compose와 Spring의 `.env` 처리 방식 차이 | Compose는 자동으로 읽지만 Spring 프로세스는 자동으로 읽지 않는다 | 실행 가이드에 환경변수 전달 방법을 명시했다 |
| CI 워크플로를 병합 전에 수동 실행할 수 없음 | GitHub API가 기본 브랜치에 없는 새 워크플로를 찾지 못했다 | Draft PR #1의 `pull_request` 이벤트에서 최초 원격 실행 성공을 확인했다 |

## 7. 현재 환경의 한계

- Docker를 실행할 수 없는 환경에서는 통합 테스트를 수행할 수 없다.
- Windows와 Linux에서 독립적인 재현 검증을 아직 하지 않았다.
- 다른 팀원이 완전히 새로운 디렉터리에서 문서만 보고 재현한 결과가 아직 없다.
- GitHub Actions의 Ubuntu 호스팅 러너는 검증했지만 다른 CI 운영체제와 자체 호스팅 러너는 검증하지 않았다.
- 프론트엔드 개발 환경, 운영 프로필, 배포 환경은 이번 요구사항 범위에 아직 포함되지 않았다.
- 데이터베이스 스키마와 마이그레이션 정책은 실제 기능과 테이블이 생길 때 결정해야 한다.
- 코드 스타일은 IDE 설정을 공유하지만 현재 CI에서 자동 강제하지 않는다.

## 8. 팀원 환경 재현 기록

다른 팀원은 새로운 디렉터리에 저장소를 복제하고 개발 가이드의 재현 완료 체크리스트를 수행한다. 기존 DB, IDE 실행 설정, 개인 환경변수를 재사용하지 않는다.

| 검증일 | 검증자 | OS | Java | Docker/Compose | 결과 | 발생 문제와 해결 |
| --- | --- | --- | --- | --- | --- | --- |
| YYYY-MM-DD | 이름 | 환경 | 버전 | 버전 | 성공/실패 | 내용 |

실패도 삭제하지 않고 어떤 단계에서 무엇을 관찰했는지 기록한다. 해결 후에는 적용한 변경과 재검증 결과를 같은 행 또는 후속 기록으로 남긴다.

## 9. 완료 기준 점검

- [x] 제품을 개발, 빌드, 테스트, 실행할 수 있는 기본 환경이 있다.
- [x] 실행 방법과 필요한 설정이 저장소에 문서화되어 있다.
- [x] 기술 선택 이유, 대안, 트레이드오프, 한계를 ADR에 기록했다.
- [x] 초기 구성 환경에서 MySQL, 애플리케이션, 테스트, 전체 빌드를 검증했다.
- [ ] 다른 팀원이 문서만 보고 새로운 환경에서 재현하고 결과를 기록했다.
- [x] 최종 PR에서 GitHub Actions 백엔드 CI가 성공했다.
- [ ] 팀이 문서와 ADR을 리뷰하고 현재 환경을 팀의 기준으로 승인했다.

## 10. 다음 행동

1. 다른 백엔드 팀원이 새 디렉터리에서 개발 환경을 재현한다.
2. 재현 결과와 발생한 문제를 이 문서에 기록한다.
3. 팀 리뷰 후 남은 체크리스트를 완료하고 상태를 `완료`로 변경한다.
