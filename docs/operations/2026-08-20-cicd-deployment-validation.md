# CI/CD 배포 검증 기록

- 문서 성격: 시점 고정
- 갱신 정책: 당시 확인한 관측값은 고정한다. 작업 상태와 후속 조치는 Issue #110에서 관리한다
- 검증일: 2026-08-20
- 관련 이슈: [#110 CI/CD 배포 검증을 구조적으로 강제한다](https://github.com/woowacourse-teams/2026-jachwi-sunbae/issues/110)

## 목적과 범위

브랜치 보호가 검증되지 않은 커밋의 병합을 막는지, 백엔드와 프론트엔드가 이번 배포의 산출물을 실제로 제공하는지, 백엔드 배포 실패 뒤 직전 정상 리비전이 실제로 복구되는지 dev 환경에서 확인한다.

prod에서 의도적인 실패를 만들지 않았다. main과 develop에 적용된 GitHub ruleset의 공통 설정과 dev 실배포를 검증 범위로 삼았다.

## 검증 결과

| 항목 | 판정 | 근거 |
| --- | --- | --- |
| PR을 거치지 않은 main·develop 변경 차단 | 충족 | active ruleset `Protect main and develop`이 두 브랜치에 PR을 요구한다 |
| 필수 상태 검사 | 충족 | `Build`, `Check frontend`, `Check docs consistency`를 strict required status check로 요구한다 |
| 우회·강제 푸시·삭제 차단 | 충족 | bypass actor가 없고 `non_fast_forward`, `deletion` 규칙이 활성화되어 있다 |
| 백엔드 이번 리비전 검증 | 충족 | JAR의 `build.commit`과 `deployment-revision.txt`가 다르면 `ValidateService`가 실패한다 |
| 백엔드 자동 롤백 실검증 | 충족 | 실패 리비전이 잠시 서빙된 뒤 직전 정상 SHA가 다시 서빙되는 것을 확인했다 |
| 백엔드 정상 복구 배포 | 충족 | 복구 PR의 머지 SHA와 dev `/actuator/info`의 `build.commit`이 일치했다 |
| 프론트엔드 이번 번들 검증 | 충족 | 현재 develop 소스로 만든 번들 파일명과 dev `index.html`의 참조가 일치하고 배포 검증 스크립트가 통과했다 |

## 브랜치 보호와 CI

ruleset `Protect main and develop`은 `refs/heads/main`, `refs/heads/develop`에 적용되고 우회 사용자가 없다. 세 required check는 PR과 두 보호 브랜치의 push에서 항상 생성된다.

백엔드와 프론트엔드 CI는 워크플로 자체를 경로 필터로 생략하지 않는다. 대신 각 워크플로의 변경 감지 단계가 수정 디렉터리를 확인한다. 백엔드만 바뀌면 `Build`가 실제 빌드와 테스트를 수행하고 `Check frontend`는 프론트 검사를 생략한 뒤 성공 상태를 보고한다. 프론트엔드만 바뀌면 반대로 동작한다. 따라서 관련 없는 빌드를 반복하지 않으면서 required check가 `Pending`에 남지 않는다.

## 백엔드 실패와 자동 롤백

실패 검증 PR [#112](https://github.com/woowacourse-teams/2026-jachwi-sunbae/pull/112)에서 `build.commit`을 유효한 형식의 zero SHA로 고정했다. GitHub 필수 검사는 통과하지만 CodeDeploy `ValidateService`가 실제 배포 SHA와 실행 중인 SHA의 불일치를 감지하도록 만든 변경이다.

| 시각(KST) | dev `/actuator/info`의 `build.commit` | 의미 |
| --- | --- | --- |
| 20:11:29 | `1e1e4f5bfcadc8ee194179cacc700e0135746f94` | 직전 정상 리비전 |
| 20:14:38 | `0000000000000000000000000000000000000000` | 실패 검증용 새 프로세스가 실행됨 |
| 20:15:08 | `1e1e4f5bfcadc8ee194179cacc700e0135746f94` | 자동 롤백 뒤 직전 정상 리비전이 복구됨 |

실패 검증용 변경은 복구 PR [#113](https://github.com/woowacourse-teams/2026-jachwi-sunbae/pull/113)에서 즉시 제거했다. 20:28:51에 dev health가 `UP`이고 `/actuator/info`가 복구 머지 SHA `17d424bc6b816bdc14e06d003878d314157b1f25`를 반환하는 것을 확인했다.

따라서 롤백 실행의 성공 표시뿐 아니라 직전 리비전의 실제 서빙과 정상 코드의 후속 배포까지 확인했다. 상세 실행 기록은 [Issue #110 검증 댓글](https://github.com/woowacourse-teams/2026-jachwi-sunbae/issues/110#issuecomment-5355252626)에 있다.

## 프론트엔드 배포 검증

현재 develop 소스를 dev 빌드 환경값으로 production 빌드했다. 로컬 `dist/index.html`과 실제 `https://dev.jachwi-sunbae.kr/index.html`이 모두 다음 번들을 참조했다.

```text
/main.3b5b1af98e8900d3e452.js
```

`frontend/deploy/verify-deployment.sh`로 두 `index.html`을 비교했고 검증이 통과했다. 실제 번들 URL도 HTTP 200을 반환했다. dev `index.html`의 `Last-Modified`는 복구 PR 배포 시각과 같은 2026-08-20 11:26 UTC였다.
