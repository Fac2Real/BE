# JPA에서 zone_hist 삽입시에 PK가 없어서 오류 발생 -> JPA GenerationType.IDENTITY 추가에 따른 스키마 수정
ALTER TABLE `zone_hist`
MODIFY COLUMN `id` BIGINT(20) NOT NULL AUTO_INCREMENT;

# 초기 작업자의 위치를 표시하기 위한 빈공간 생성
INSERT INTO zone_info (zone_id, zone_name) VALUES
   ('00000000000000-000', '대기실')