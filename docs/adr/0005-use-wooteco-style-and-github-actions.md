# ADR-0005: 코드 스타일과 CI 검증 방식을 선택한다

- 상태: 승인
- 결정일: 2026-08-04
- 참여자: 자취선배 백엔드 팀

## 맥락

팀원별 IDE 설정이 다르면 기능과 무관한 포맷 변경이 리뷰를 방해한다. 또한 특정 팀원의 로컬 환경에서만 빌드가 성공하면 팀이 개발 환경을 소유한다고 보기 어렵다.

## 결정

- 우테코 공식 IntelliJ Java 코드 스타일 XML을 원본 그대로 저장소에서 관리한다.
- `.editorconfig`로 IDE 공통 줄바꿈, 공백, Java 들여쓰기, 120자 기준을 적용한다.
- GitHub Actions에서 백엔드 CI를 실행한다.
- CI는 Java 21과 Gradle Wrapper를 사용해 `./gradlew clean build --no-daemon`을 실행한다.
- `pull_request → main`과 `push → main`에서 백엔드 또는 워크플로가 변경될 때 실행한다.
- 중복 실행은 취소하고 `contents: read` 최소 권한만 부여한다.

## 근거

- 팀이 이미 사용해 본 우테코 스타일이라 도입 학습 비용이 낮다.
- 공식 XML은 import 순서, wildcard import 방지, 중괄호와 줄바꿈 같은 IntelliJ 세부 설정을 공유한다.
- EditorConfig는 IntelliJ 외의 편집기에서도 기본 형식을 맞춘다.
- GitHub Actions는 저장소 권한과 PR 흐름에 바로 연결되며 별도 CI 서버 운영이 필요 없다.
- CI에서 Testcontainers까지 실행하면 개인 로컬 MySQL에 의존하지 않는지 검증할 수 있다.

## 검토한 대안

| 대안 | 장점 | 우려 | 선택하지 않은 이유 |
| --- | --- | --- | --- |
| IntelliJ 기본 스타일 | 별도 설정이 필요 없다 | 팀원 버전과 개인 설정에 따라 결과가 달라진다 | 기존에 합의한 우테코 스타일을 재사용하는 편이 일관적이다 |
| Google Java Style | 널리 쓰이고 자동 포맷 도구가 많다 | 기존 팀 스타일과 포맷 차이가 크다 | 전환 이점보다 초기 적응 비용이 크다 |
| Checkstyle 또는 Spotless 즉시 도입 | CI에서 형식을 강제할 수 있다 | 규칙 조정과 기존 코드 수정 비용이 추가된다 | 초기에는 합의와 IDE 적용부터 확인한다 |
| 로컬 빌드만 확인 | 설정이 가장 단순하다 | 누락과 환경 차이를 자동으로 발견할 수 없다 | 팀 소유 환경의 완료 기준을 충족하기 어렵다 |
| 별도 Jenkins 운영 | 높은 자유도와 사내 환경 통제가 가능하다 | 서버 운영과 보안 관리가 필요하다 | 현재 규모에서는 관리 비용이 과도하다 |

## 결과와 트레이드오프

### 기대하는 결과

- 포맷 차이보다 코드 의미에 집중해 리뷰한다.
- PR마다 동일한 Java와 Gradle 기준으로 빌드와 테스트를 확인한다.
- 특정 팀원의 로컬 설정에만 의존하지 않는다.

### 감수하는 비용과 한계

- 현재 CI는 코드 포맷을 자동으로 실패시키지 않는다.
- GitHub Actions 장애, 정책, 사용량 제한의 영향을 받는다.
- Testcontainers 때문에 일반 단위 테스트만 실행하는 CI보다 시간이 더 걸린다.
- 새 워크플로는 기본 브랜치 병합 전 수동 실행이 제한되므로 최초 PR에서 원격 결과를 확인해야 한다.

## 검증 방법

- 공식 우테코 XML과 저장소 파일의 Git blob hash가 같은지 확인한다.
- 로컬에서 CI와 같은 전체 빌드 명령을 실행한다.
- 최초 PR에서 GitHub Actions `Build and test`가 성공하는지 확인한다.

## 재검토 조건

- 포맷 차이로 인한 리뷰 문제가 반복된다.
- 팀원이 IDE 스타일을 일관되게 적용하지 못한다.
- CI 시간이 PR 흐름을 방해하거나 GitHub Actions 사용 제약이 발생한다.

## 참고 자료

- [우테코 IntelliJ Java 코드 스타일](https://github.com/woowacourse/woowacourse-docs/blob/main/styleguide/java/intellij-java-wooteco-style.xml)
- [Gradle setup-gradle 공식 가이드](https://github.com/gradle/actions/blob/main/docs/setup-gradle.md)
