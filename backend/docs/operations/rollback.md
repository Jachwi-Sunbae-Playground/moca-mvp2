# 롤백

- 상태: 동작 중
- 문서 성격: 파생
- 대조 대상: 실제 CodeDeploy 배포 그룹 설정, `backend/deploy/`

배포 구성은 [배포](deployment.md)에 있다.

## 애플리케이션 롤백

`ValidateService`가 실패하면 배포가 중단된다. `/actuator/health`가 최대 4분 안에 `UP`이 되지 않거나, 서비스가 죽거나, `/actuator/info`의 `build.commit`이 이번 배포 SHA와 다르면 실패로 처리하고 최근 로그를 남긴다. 따라서 기동하지 못한 리비전과 옛 프로세스가 응답한 배포를 성공으로 기록하지 않는다.

### 자동 롤백은 CodePipeline이 한다

배포가 실패하면 직전 정상 리비전으로 자동으로 되돌아간다. 다만 **이 동작의 주체는 CodeDeploy가 아니라 CodePipeline이다.**

| 계층 | 설정 | 동작 |
| --- | --- | --- |
| CodePipeline 스테이지 | 자동 롤백 **활성** | 실패한 스테이지를 직전 성공 실행으로 되돌린다 |
| CodeDeploy 배포 그룹 | 롤백 **비활성** | 자체 롤백을 하지 않는다 |

**파이프라인 밖에서 수동으로 배포하면 자동 롤백이 동작하지 않는다.** 장애 상황에서 이 차이를 모르면 잘못 기대하게 된다.

롤백 실행에도 초록불이 뜰 수 있으므로 실행 목록의 `AutomatedRollback` 표시와 `FailedPipelineExecutionId`를 확인한다. 원인을 확인할 때는 실패한 실행을 따로 연다.

수동으로 되돌릴 때는 CodeDeploy에서 직전 정상 리비전을 다시 배포한다. 되돌리는 커밋을 올리는 방법보다 빠르며, 같은 훅을 그대로 거치므로 health와 리비전 확인도 동일하게 이뤄진다. 이때 **환경에 맞는 배포 그룹**을 고른다. prod는 `jachwi-sunbae-codeDeploy-group`, dev는 `jachwi-sunbae-dev-group`이다.

### 롤백 성공 판정

롤백 실행의 초록불만으로 복구를 판정하지 않는다. 직전 정상 실행의 소스 SHA를 `GOOD_REVISION`으로 기록한 뒤 실제 응답과 비교한다.

```bash
curl -fsS https://dev-api.jachwi-sunbae.kr/actuator/info
```

응답의 `build.commit`이 `GOOD_REVISION`과 같아야 백엔드 롤백이 끝난 것이다. 프론트엔드는 직전 정상 실행의 `index.html`이 참조한 JS·CSS 파일명과 현재 응답을 비교한다. [프론트엔드 배포](../../../frontend/docs/deployment.md)의 배포 검증 명령을 사용한다.

### dev에서 자동 롤백을 검증하는 절차

1. dev의 `/actuator/info`에서 현재 정상 SHA를 `GOOD_REVISION`으로 기록한다.
2. CI는 통과하지만 `build.commit`이 다른 40자리 값이 되도록 만든 테스트용 변경을 `develop`에 병합한다.
3. `ValidateService`의 `expected`와 `actual` 불일치로 배포가 실패하는지 확인한다.
4. `AutomatedRollback` 실행이 끝난 뒤 dev의 `/actuator/info`가 다시 `GOOD_REVISION`을 반환하는지 확인한다.
5. 실패한 실행 ID, 롤백 실행 ID, `GOOD_REVISION`, 검증 시각을 이슈에 기록하고 테스트용 변경을 즉시 되돌린다.

이 절차는 dev에서만 실행한다. prod에서 의도적인 실패를 만들지 않는다.

2026-08-20에 이 절차를 실행해 실패 리비전의 기동과 직전 정상 리비전의 실제 복구를 확인했다. SHA와 시각, 복구 PR은 [CI/CD 배포 검증 기록](../../../docs/operations/2026-08-20-cicd-deployment-validation.md)에 남긴다.

데이터 손실 가능성이 있는 작업은 즉시 실행하지 않고 영향 범위와 복구 가능성을 먼저 확인한다.
