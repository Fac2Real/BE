ALTER TABLE notify_log
    DROP FOREIGN KEY FK_notify_log_to_abn_log;

ALTER TABLE notify_log
    DROP FOREIGN KEY FK_notify_log_to_wearable_info;

ALTER TABLE notify_log
    DROP PRIMARY KEY;

ALTER TABLE notify_log
    MODIFY abnormal_id BIGINT NULL;


ALTER TABLE notify_log
    ADD id BIGINT AUTO_INCREMENT PRIMARY KEY ;

ALTER TABLE notify_log
    ADD status BIT(1) NULL;

ALTER TABLE notify_log
    ADD target VARCHAR(200) NULL;

ALTER TABLE notify_log
    ADD trigger_type SMALLINT NULL;

ALTER TABLE notify_log
    DROP COLUMN recipient_id;

