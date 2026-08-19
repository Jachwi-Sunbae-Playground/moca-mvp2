-- Access Token만 사용하므로 Refresh Token 저장 테이블을 제거한다.
DROP TABLE IF EXISTS refresh_tokens;

-- 기존 스키마의 OAuth subject/display_name은 데이터 손실 없이 레거시로 비활성화한다.
ALTER TABLE members
    MODIFY COLUMN oauth_provider VARCHAR(20) NULL,
    MODIFY COLUMN oauth_subject VARCHAR(255) NULL,
    MODIFY COLUMN display_name VARCHAR(100) NULL;
