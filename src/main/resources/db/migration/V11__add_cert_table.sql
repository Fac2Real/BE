CREATE TABLE `cert_info` (
                             `cert_id`	VARCHAR(100)	NOT NULL,
                             `grade`	VARCHAR(100)	NULL	COMMENT 'A / B / C',
                             `issued_at`	DATETIME	NULL,
                             `updated_at`	DATETIME	NULL,
                             `valid_until`	DATETIME	NULL,
                             `cert_score`	INT	NULL	COMMENT '1~100점',
                             `description`	TEXT	NULL,
                             `status`	VARCHAR(100)	NULL	COMMENT '유효 / 만료 / 취소'
);

ALTER TABLE `cert_info` ADD CONSTRAINT `PK_CERT_INFO` PRIMARY KEY (
                                                                   `cert_id`
    );

