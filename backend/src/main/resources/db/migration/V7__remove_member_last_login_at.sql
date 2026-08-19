-- 회원의 마지막 로그인 시각은 서비스 기능에 사용하지 않으므로 저장하지 않는다.
ALTER TABLE members
    DROP COLUMN last_login_at;
