# MVP2 전환 기준

- 상태: 1~5단계 완료
- 기준일: 2026-08-22
- 문서 성격: 파생
- 대조 대상: 저장소 코드·설정, GitHub Issue, `mvp1-baseline` 태그

## 목적과 범위

Moca MVP2는 자취선배 MVP1을 독립된 개인 실험 공간으로 옮겨, 기존 기능을 빠르게 수정하고 지도·공공데이터 기능을 추가하기 위한 저장소다. MVP2 기능 개발이 끝날 때까지 로컬에서만 개발하며 AWS 리소스를 만들거나 실제 배포하지 않는다.

이번 전환은 다음 범위만 완료한다.

1. 원본 `develop`의 Git 이력을 새 레포 `main`으로 이전한다.
2. 제거·유지·대체 항목을 확정한다.
3. Flyway를 제거하고 새 DB 초기화 기준을 만든다.
4. 향후 단일 EC2 최소 배포의 저장소 기반만 준비한다.
5. 개인 개발에 맞게 브랜치 규칙과 CI를 단순화한다.

MVP1 요구사항·정책·ERD·API 명세 통합과 MVP2 기능 정의는 후속 대화와 Issue에서 작성한다.

## 기준선

| 항목 | 값 |
| --- | --- |
| 원본 저장소 | `woowacourse-teams/2026-jachwi-sunbae` |
| 원본 기준 브랜치 | `develop` |
| 원본 기준 커밋 | `021a1b8b2565323a8963f6e8fe6dc72bb33eff0b` |
| 새 저장소 | `Jachwi-Sunbae-Playground/moca-mvp2` |
| 새 기본 브랜치 | `main` |
| 기준 태그 | `mvp1-baseline` |

새 저장소의 `main`은 기준 커밋까지 도달 가능한 전체 Git 이력을 보존한다. 원본 저장소는 `upstream` 읽기용 remote로만 남기고 push URL은 비활성화한다.

## 제거·유지·대체

| 구분 | 항목 | 처리 | 이유 또는 대체 기준 |
| --- | --- | --- | --- |
| Git | 원본 커밋 이력 | 유지 | MVP1 변경 맥락과 `git blame`을 보존한다 |
| Git | `develop` 통합 브랜치 | 제거 | 개인 저장소는 `main`과 짧은 작업 브랜치만 사용한다 |
| 문서 | 기존 ADR·실험·피벗·MVP1 배포 검증 | 유지 | 당시 판단을 보여주는 시점 고정 기록이다 |
| DB | Flyway 의존성과 누적 migration | 제거 | 새 DB만 사용하며 현재 스키마는 단일 SQL로 재생성한다 |
| DB | 현재 코드가 사용하지 않는 레거시 테이블 | 제거 | 새 스키마의 정본을 실제 코드 범위와 맞춘다 |
| DB | `system_meno_id` 오타 | 대체 | `system_memo_item_id`로 스키마와 JDBC SQL을 함께 고친다 |
| 로컬 | MySQL·MinIO Compose | 유지·수정 | 빈 MySQL 자동 초기화와 S3 호환 로컬 개발에 사용한다 |
| 식별자 | 애플리케이션·DB·프론트 패키지 이름 | 대체 | 새 환경에서 `moca-mvp2` 이름을 사용한다 |
| 설정 | 실제로 Base64를 해석하지 않는 `JWT_SECRET_BASE64` | 대체 | 의미에 맞게 `JWT_SECRET`으로 바꾼다 |
| 사진 | S3 SDK와 비공개 저장 정책 | 유지·완성 | MVP2에서 실제 MinIO/S3 객체 저장과 소유자 검증 조회를 완성했다 |
| 배포 | ALB·WAF·RDS·CloudFront·CodePipeline·CodeDeploy | 제거 | 개인 계정의 단일 EC2·Caddy·로컬 MySQL·S3·Actions로 대체한다 |
| 배포 | `main` 병합 자동 배포 | 제거 | MVP2 기능 완료 전 오배포를 막고 수동 배포만 준비한다 |
| CI | push 중복 검사와 전체 FE 검사 | 제거 | PR의 변경 영역에서 백엔드 테스트 또는 프론트 빌드만 수행한다 |
| 품질 | 문서 정합성 검사와 PR | 유지 | 개인·AI 개발에서도 저렴하게 회귀와 맥락 누락을 잡는다 |
| 협업 | 승인 1명 필수 | 제거 | 본인 확인과 CI 성공을 병합 기준으로 삼는다 |

## 추적 Issue

- [#1 개인 개발 전환 기반](https://github.com/Jachwi-Sunbae-Playground/moca-mvp2/issues/1)
- [#2 개인 개발 규칙과 문서 정합성](https://github.com/Jachwi-Sunbae-Playground/moca-mvp2/issues/2)
- [#3 Flyway 제거와 단일 스키마](https://github.com/Jachwi-Sunbae-Playground/moca-mvp2/issues/3)
- [#4 로컬 개발과 향후 최소 AWS 배포 기반](https://github.com/Jachwi-Sunbae-Playground/moca-mvp2/issues/4)
- [#5 MVP1 기준선과 개발 이력 이전](https://github.com/Jachwi-Sunbae-Playground/moca-mvp2/issues/5)

각 Issue의 체크리스트와 PR을 진행 상태의 정본으로 사용한다. 문서에는 완료된 기준과 장기적으로 유지할 결정만 남긴다.
