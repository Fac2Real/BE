CREATE TABLE `equip_hist` (
    `id`	BIGINT	NOT NULL AUTO_INCREMENT,
    `equip_id`	VARCHAR(100)	NOT NULL,
    `date`	TIMESTAMP	NULL,
    `type`	VARCHAR(20)	NULL	COMMENT 'UPDATE(설비 점검) ACCIDENT(ML에서 발생한 경고)',
    primary key (id),
    CONSTRAINT FK_equip_info_to_equip_hist FOREIGN KEY (equip_id) references equip_info(equip_id)
);

