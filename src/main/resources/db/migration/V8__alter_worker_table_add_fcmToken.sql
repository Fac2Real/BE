ALTER TABLE worker_info
    ADD COLUMN fcm_token VARCHAR(200);


ALTER TABLE abn_log
    ADD COLUMN danger_level smallint;