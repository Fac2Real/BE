-- 기존 컬럼 삭제
ALTER TABLE equip_hist
DROP COLUMN date,
DROP COLUMN type;

-- 새로운 컬럼 추가
ALTER TABLE equip_hist
ADD COLUMN accident_date DATE NOT NULL COMMENT '예상 점검일자',
ADD COLUMN check_date DATE NULL COMMENT '실제 점검일자'; 