ALTER TABLE sensor_info
    ADD CONSTRAINT FK_SENSOR_INFO_ON_EQUIP FOREIGN KEY (equip_id) REFERENCES equip_info (equip_id);

ALTER TABLE sensor_info
    ADD CONSTRAINT FK_SENSOR_INFO_ON_ZONE FOREIGN KEY (zone_id) REFERENCES zone_info (zone_id);

ALTER TABLE wearable_hist
    ADD CONSTRAINT FK_WEARABLE_HIST_ON_WEARABLE FOREIGN KEY (wearable_id) REFERENCES wearable_info (wearable_id);

ALTER TABLE wearable_hist
    ADD CONSTRAINT FK_WEARABLE_HIST_ON_WORKER FOREIGN KEY (worker_id) REFERENCES worker_info (worker_id);

ALTER TABLE wearable_hist
    ADD CONSTRAINT FK_WEARABLE_HIST_ON_ZONE FOREIGN KEY (zone_id) REFERENCES zone_info (zone_id);

ALTER TABLE zone_hist
    ADD CONSTRAINT FK_ZONE_HIST_ON_WORKER FOREIGN KEY (worker_id) REFERENCES worker_info (worker_id);

ALTER TABLE zone_hist
    ADD CONSTRAINT FK_ZONE_HIST_ON_ZONE FOREIGN KEY (zone_id) REFERENCES zone_info (zone_id);

ALTER TABLE equip_info
    ADD CONSTRAINT FK_EQUIP_INFO_ON_ZONE FOREIGN KEY (zone_id) REFERENCES zone_info (zone_id);

ALTER TABLE abn_log
    ADD CONSTRAINT FK_ABN_LOG_ON_ZONE FOREIGN KEY (zone_id) REFERENCES zone_info (zone_id);

ALTER TABLE notify_log
    ADD CONSTRAINT FK_NOTIFY_LOG_ON_ZONE FOREIGN KEY (abnormal_id) REFERENCES abn_log (id);

ALTER TABLE `wearable_info`
    CHANGE COLUMN `created_id` `created_at` TIMESTAMP NOT NULL AFTER `wearable_id`;