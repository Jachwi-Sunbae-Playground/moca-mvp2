-- 기존 운영 회원과 소유 데이터를 유지한 채 닉네임 인증 자격정보 테이블을 추가한다.
-- 애플리케이션이 NFKC 정규화와 중복 처리를 적용해 자격정보가 없는 회원만 보강한다.

CREATE TABLE IF NOT EXISTS nickname_credentials
(
    member_id     BIGINT       NOT NULL,
    nickname      VARCHAR(100) NOT NULL,
    nickname_key  VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    password_hash VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (member_id),
    CONSTRAINT uk_nickname_credentials_key UNIQUE (nickname_key),
    CONSTRAINT fk_nickname_credentials_member
        FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
