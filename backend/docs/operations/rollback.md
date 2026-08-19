# 롤백

- 상태: 동작 중
- 문서 성격: 파생
- 대조 대상: 실제 CodeDeploy 배포 그룹 설정, `backend/deploy/`

배포 구성은 [배포](deployment.md)에 있다.

## 애플리케이션 롤백

`ValidateService`가 실패하면 배포가 중단된다. `/actuator/health`가 최대 4분 안에 `UP`이 되지 않거나 서비스가 죽으면 실패로 처리하고 최근 로그를 남긴다. 따라서 기동하지 못하는 리비전이 배포 성공으로 기록되지 않는다.

### 자동 롤백은 CodePipeline이 한다

배포가 실패하면 직전 정상 리비전으로 자동으로 되돌아간다. 다만 **이 동작의 주체는 CodeDeploy가 아니라 CodePipeline이다.**

| 계층 | 설정 | 동작 |
| --- | --- | --- |
| CodePipeline 스테이지 | 자동 롤백 **활성** | 실패한 스테이지를 직전 성공 실행으로 되돌린다 |
| CodeDeploy 배포 그룹 | 롤백 **비활성** | 자체 롤백을 하지 않는다 |

**파이프라인 밖에서 수동으로 배포하면 자동 롤백이 동작하지 않는다.** 장애 상황에서 이 차이를 모르면 잘못 기대하게 된다.

롤백 실행에도 초록불이 뜰 수 있으므로 실행 목록의 `AutomatedRollback` 표시와 `FailedPipelineExecutionId`를 확인한다. 원인을 확인할 때는 실패한 실행을 따로 연다.

수동으로 되돌릴 때는 CodeDeploy에서 환경에 맞는 직전 정상 리비전을 다시 배포한다. prod는 `jachwi-sunbae-codeDeploy-group`, dev는 `jachwi-sunbae-dev-group`을 사용한다.

데이터 손실 가능성이 있는 작업은 즉시 실행하지 않고 영향 범위와 복구 가능성을 먼저 확인한다.
