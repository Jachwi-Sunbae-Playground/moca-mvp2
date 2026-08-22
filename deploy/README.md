# 최소 배포 파일

- 상태: 준비됨·미배포
- 문서 성격: 파생
- 대조 대상: 이 디렉터리와 `.github/workflows/deploy-production.yml`

이 디렉터리는 MVP2 기능 개발이 끝난 뒤 단일 EC2에 처음 배포할 때 사용한다. 현재 AWS 리소스는 만들지 않았고 배포 워크플로도 실행하지 않는다. 전체 구성과 준비 조건은 [MVP2 배포 아키텍처](../docs/operations/mvp2-deployment-architecture.md)를 따른다.

| 파일 | 책임 |
| --- | --- |
| `Caddyfile.example` | HTTPS, SPA 정적 파일, `/api` 역방향 프록시 |
| `compose.yaml` | EC2 내부 MySQL 8.4와 영속 볼륨 |
| `moca-backend.service` | Spring Boot systemd 서비스 |
| `scripts/deploy-release.sh` | 릴리스 교체, health 확인과 애플리케이션 롤백 |

첫 배포 전에는 도메인, EC2, 비공개 사진 S3 버킷, 릴리스 S3 버킷, GitHub OIDC 역할과 SSM 접근을 별도로 준비한다. 비밀값은 저장소나 Actions 명령에 넣지 않고 EC2의 `/etc/moca/app.env`에서 관리한다.
