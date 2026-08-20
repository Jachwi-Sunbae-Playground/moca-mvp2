-- 현재 매물 저장 모델이 더 이상 사용하지 않는 v1 레거시 구분값을 선택값으로 전환한다.
-- 기존 데이터와 컬럼은 보존하고 후속 스키마 정리 전까지 신규 저장 SQL과 호환한다.
ALTER TABLE properties
    MODIFY COLUMN discovery_source_type VARCHAR(20) NULL;
