# MVP2 롤백

- 상태: 저장소 기반 준비 완료, 미검증
- 문서 성격: 파생
- 대조 대상: `deploy/scripts/deploy-release.sh`, 실제 EC2 릴리스와 DB 백업 구성

애플리케이션과 DB 복구를 분리한다. 전체 배포 기준은 [MVP2 배포 아키텍처](../../../docs/operations/mvp2-deployment-architecture.md)를 따른다.

## 애플리케이션

각 릴리스는 `/opt/moca/releases/<Git SHA>`에 풀고 `/opt/moca/current` 심볼릭 링크를 바꾼다. 재시작 뒤 내부 health가 실패하면 배포 스크립트가 링크를 직전 경로로 돌리고 서비스를 다시 시작한다.

자동 복구 뒤에도 다음을 직접 확인한다.

```bash
readlink -f /opt/moca/current
systemctl status moca-backend.service
curl --fail http://127.0.0.1:8080/actuator/health
```

수동 롤백은 정상 릴리스 경로를 확인하고 `current` 링크를 바꾼 뒤 `moca-backend.service`를 재시작한다. 원인을 GitHub Issue에 남기고 health와 핵심 사용자 흐름을 다시 확인한다.

## 프론트엔드

프론트 정적 파일도 같은 릴리스 안에 있으므로 애플리케이션 링크 롤백과 함께 돌아간다. 공개 도메인의 `/`, `/properties`와 OAuth callback 경로를 새로고침해 SPA fallback까지 확인한다.

## 데이터베이스

애플리케이션 릴리스 롤백은 MySQL 데이터나 스키마를 되돌리지 않는다. 첫 공개 배포 전까지는 스키마 변경 시 DB를 재생성할 수 있지만, 실제 사용자 데이터가 생긴 뒤에는 다음 원칙을 적용한다.

- 데이터가 있는 DB에 `001-schema.sql`을 다시 실행하지 않는다.
- 파괴적인 SQL을 배포 스크립트에 넣지 않는다.
- 복구 전에 장애 시점, 마지막 정상 백업과 백업 이후 데이터 손실 범위를 확인한다.
- 복원은 별도 DB에서 검증한 뒤 서비스 중단과 사용자 영향을 기록하고 실행한다.
- 보존 데이터가 생긴 뒤의 스키마 변경은 [ADR-0009](../adr/0009-use-disposable-database-schema.md)의 재검토 조건에 따라 마이그레이션 체계를 먼저 도입한다.
