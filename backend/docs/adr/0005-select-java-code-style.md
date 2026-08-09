# ADR-0005: Java 코드 스타일을 선택한다

- 상태: 승인
- 결정일: 2026-08-04
- 참여자: 자취선배 백엔드 팀
- 문서 성격: 시점 고정(부분)
- 갱신 정책: 맥락·결정·근거·검토한 대안은 고정한다. 결과와 재검토 조건만 갱신한다

## 맥락

팀원별 IDE 설정이 다르면 기능과 무관한 포맷 변경이 리뷰를 방해한다. 팀이 이미 익숙한 기준을 재사용해 코드 의미에 집중할 수 있는 공통 스타일이 필요하다.

## 결정

- 우테코 공식 IntelliJ Java 코드 스타일 XML을 원본 그대로 `backend/config/code-style`에서 관리한다.
- 루트 `.editorconfig`로 저장소 공통 줄바꿈과 공백을 맞추고 Java 들여쓰기와 120자 기준을 적용한다.
- wildcard import를 사용하지 않는다.
- 현재는 Checkstyle이나 Spotless로 포맷을 강제하지 않고 IDE 설정과 리뷰로 적용한다.

## 근거

- 팀이 사용해 본 우테코 스타일이라 도입 학습 비용이 낮다.
- 공식 XML은 import 순서, wildcard import 방지와 줄바꿈 같은 IntelliJ 세부 설정을 공유한다.
- EditorConfig는 IntelliJ 외 편집기에서도 기본 형식을 맞춘다.
- Java 전용 설정을 `backend`에 두면 이후 프론트엔드 전용 도구 설정과 섞이지 않는다.

## 검토한 대안

| 대안 | 장점 | 우려 | 선택하지 않은 이유 |
| --- | --- | --- | --- |
| IntelliJ 기본 스타일 | 별도 설정이 필요 없다 | 버전과 개인 설정에 따라 결과가 달라진다 | 팀 기준을 명시하기 어렵다 |
| Google Java Style | 널리 사용되고 자동 포맷 도구가 많다 | 기존 팀 스타일과 차이가 크다 | 전환 이점보다 적응 비용이 크다 |
| Checkstyle 또는 Spotless 즉시 도입 | CI에서 형식을 강제한다 | 규칙 조정과 초기 설정 비용이 생긴다 | 먼저 합의와 IDE 적용 효과를 확인한다 |

## 결과와 트레이드오프

### 기대하는 결과

- 포맷 차이보다 코드 의미에 집중해 리뷰한다.
- IDE가 달라도 기본 줄바꿈과 들여쓰기를 일관되게 유지한다.

### 감수하는 비용과 한계

- 현재 CI는 코드 포맷 위반을 자동으로 검출하지 않는다.
- 팀원이 IntelliJ 스타일을 직접 불러와야 한다.

## 검증 방법

- 팀원이 `backend/config/code-style/intellij-java-wooteco-style.xml`을 불러와 같은 결과로 포맷하는지 확인한다.
- 포맷 차이로 인한 리뷰 문제가 반복되는지 회고에서 확인한다.

## 재검토 조건

- 포맷 차이가 반복되거나 IDE 설정 적용이 누락된다.
- 자동 포맷 검증 비용보다 리뷰 절감 효과가 커진다.

## 참고 자료

- [우테코 IntelliJ Java 코드 스타일](https://github.com/woowacourse/woowacourse-docs/blob/main/styleguide/java/intellij-java-wooteco-style.xml)
