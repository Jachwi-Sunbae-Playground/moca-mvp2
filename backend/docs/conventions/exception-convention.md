# 예외 컨벤션

## 기본 원칙

- 예외를 클라이언트 예외와 서버 예외로 구분한다.
- Controller는 예외를 직접 처리하지 않고 `GlobalExceptionHandler`에 맡긴다.
- 클라이언트는 `message`가 아닌 `code`로 오류를 구분한다.
- DB, 외부 API, 스택 트레이스 등 내부 정보는 응답에 노출하지 않는다.

## 예외 구조

```text
common/exception
├── JachwiException
├── client
│   ├── ClientException
│   ├── InvalidCommandException
│   ├── BusinessRuleViolationException
│   ├── ResourceNotFoundException
│   └── AuthenticationFailedException
├── server
│   ├── ServerException
│   ├── DataInconsistencyException
│   ├── ExternalServiceException
│   └── UpstreamServiceException
├── errorcode
│   └── ErrorCode
├── ErrorResponse
└── GlobalExceptionHandler
```

`JachwiException`이 최상위이며 `ClientException`과 `ServerException`이 이를 상속한다.

## 예외를 던지는 규칙

- `Exception`, `RuntimeException`을 직접 던지지 않는다.
- 도메인 내부 불변식 위반은 `IllegalArgumentException` / `IllegalStateException`으로 표현한다. 이 예외는 클라이언트 응답 계약에 포함되지 않으며, 핸들러까지 도달하면 `500`으로 처리한다(설계 누락으로 간주).
- 클라이언트가 구분해야 하는 모든 오류는 `JachwiException` 하위 예외로 던지고 `ErrorCode`를 부여한다.
- 서비스 레이어에서 `IllegalArgumentException`을 try-catch로 잡아 커스텀 예외로 변환하지 않는다. 발생 지점에서 처음부터 커스텀 예외를 던진다.
- `InvalidCommandException`은 `IllegalArgumentException`을 상속하지 않는다.
- 예외 메시지에 개인정보나 내부 구현 정보를 넣지 않는다.

## HTTP 변환

`@RestControllerAdvice` 한 곳에서 예외를 HTTP 오류로 변환한다.

| 예외 | 상태 코드 | 응답 |
| --- | --- | --- |
| `ClientException` 계열 | `4xx` | `ErrorCode` 기반 `ErrorResponse` |
| `ServerException` 계열 | `5xx` | `ErrorCode` 기반 `ErrorResponse` |
| 그 외 전부 | `500` | `INTERNAL_SERVER_ERROR` 폴백 |

- raw `IllegalArgumentException`과 `IllegalStateException`이 핸들러까지 도달한 것은 예외 설계 누락으로 간주하고 `400`으로 관대하게 매핑하지 않는다.
- 폴백 핸들러는 원인 추적을 위해 로그를 남긴다.

하위 예외별 기본 상태 코드는 다음과 같다.

| 예외 | 상태 코드 |
| --- | --- |
| `InvalidCommandException` | `400 Bad Request` |
| `BusinessRuleViolationException` | `400 Bad Request` |
| `ResourceNotFoundException` | `404 Not Found` |
| `DataInconsistencyException` | `500 Internal Server Error` |
| `ExternalServiceException` | `500 Internal Server Error` |

인증 필터에서 발생하는 오류는 Controller 이후의 `GlobalExceptionHandler`에 도달하지 않는다. `JachwiAuthenticationEntryPoint`가 같은 오류 응답 형식으로 다음 코드를 반환한다.

| 상황 | 상태 코드 | 오류 코드 |
| --- | --- | --- |
| Bearer Access Token 누락·잘못된 인증 스킴 | `401 Unauthorized` | `UNAUTHENTICATED` |
| Access Token 만료 | `401 Unauthorized` | `ACCESS_TOKEN_EXPIRED` |
| 서명·issuer·audience·subject·형식 오류 | `401 Unauthorized` | `ACCESS_TOKEN_INVALID` |

`UpstreamServiceException`은 Kakao Local이나 TAGO처럼 상류 시스템이 실패한 경우 사용하며 `ErrorCode`에 따라 `502 Bad Gateway`로 변환한다.

## 오류 응답

```json
{
  "code": "PROPERTY_NOT_FOUND",
  "message": "해당 매물을 찾을 수 없습니다.",
  "errors": []
}
```

- 오류 코드는 `UPPER_SNAKE_CASE`를 사용한다.
- 오류 코드는 `도메인_상태` 형식으로 작성한다.
- 검증 오류에만 `errors`에 필드별 상세 내용을 넣는다.
- authorization code, code verifier, nonce, token, 비밀번호, 비밀키, 메모, 발견 경로와 사진은 검증 오류의 `rejectedValue`에 포함하지 않는다.

## Swagger 문서화

Swagger에는 요청, 성공 응답, 검증 오류와 대표 비즈니스 오류를 문서화한다.

---

응답 형식 전반은 [API 컨벤션](api-convention.md), 패키지 위치는 [백엔드 패키지 구조](../architecture/backend-package-structure.md)를 따른다.
