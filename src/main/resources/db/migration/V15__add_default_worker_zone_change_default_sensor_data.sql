SET @waiting_room_id := '00000000000000-000';

INSERT INTO worker_zone ( worker_id, zone_id, manage_yn)
VALUES
    ('20220001', @waiting_room_id, 0),
    ('20220002', @waiting_room_id, 0),
    ('20230001', @waiting_room_id, 0),
    ('20230002', @waiting_room_id, 0),
    ('20230003', @waiting_room_id, 0),
    ('20240001', @waiting_room_id, 0),
    ('20240002', @waiting_room_id, 0),
    ('20250001', @waiting_room_id, 0),
    ('20250002', @waiting_room_id, 0),
    ('20250003', @waiting_room_id, 0),
    ('20251234', @waiting_room_id, 0),
    ('20230063', @waiting_room_id, 0),
    ('20250521', @waiting_room_id, 0),
    ('20239988', @waiting_room_id, 0);

DELETE FROM sensor_info
WHERE sensor_id IN ('UA10V-VIB-24060891',
                    'UA10V-VIB-24060892',
                    'UA10V-VIB-24060893');

INSERT INTO zone_info (zone_id, zone_name) VALUES
                                               ('20250507165750-826', 'ABS 공정 - 중합 공정'),
                                               ('20250507165750-825', 'ABS 공정 - 탈휘 공정');

INSERT INTO equip_info (equip_id, equip_name, zone_id) VALUES
                                                           ('20250507165750-826', 'empty', '20250507165750-826'),
                                                           ('20250507171316-388', '중합 설비', '20250507165750-826'),
                                                           ('20250507165750-825', 'empty', '20250507165750-825'),
                                                           ('20250507171316-387', '탈휘 설비', '20250507165750-825');

# 중합 설비(공간)
INSERT INTO sensor_info (sensor_id, sensor_type, val_unit, sensor_thres, created_at, zone_id, equip_id, iszone) VALUES
                                                                                                                    ('UA10V-VOC-24060880', 'voc', null, null, null, '20250507165750-826', '20250507165750-826', 1),
                                                                                                                    ('UA10T-TEM-24060880', 'temp', null, null, null, '20250507165750-826', '20250507165750-826', 1);

# 중합 설비(설비)
INSERT INTO sensor_info (sensor_id, sensor_type, val_unit, sensor_thres, created_at, zone_id, equip_id, iszone) VALUES
                                                                                                                    ('UA10P-APWR-24060882', 'active_power', null, null, null, '20250507165750-826', '20250507171316-388', 0),
                                                                                                                    ('UA10P-RPWR-24060882', 'reactive_power', null, null, null, '20250507165750-826', '20250507171316-388', 0),
                                                                                                                    ('UA10H-HUM-24060882', 'humid', null, null, null, '20250507165750-826', '20250507171316-388', 0),
                                                                                                                    ('UA10H-PRS-34060882', 'pressure', null, null, null, '20250507165750-826', '20250507171316-388', 0),
                                                                                                                    ('UA10T-TEM-24060881', 'temp', null, null, null, '20250507165750-826', '20250507171316-388', 0),
                                                                                                                    ('UA10V-VIB-24060882', 'vibration', null, null, null, '20250507165750-826', '20250507171316-388', 0);

# 탈휘 설비(공간)
INSERT INTO sensor_info (sensor_id, sensor_type, val_unit, sensor_thres, created_at, zone_id, equip_id, iszone) VALUES
                                                                                                                    ('UA10V-VOC-24060879', 'voc', null, null, null, '20250507165750-825', '20250507165750-825', 1),
                                                                                                                    ('UA10D-DST-24060879', 'dust', null, null, null, '20250507165750-825', '20250507165750-825', 1),
                                                                                                                    ('UA10H-HUM-24060879', 'humid', null, null, null, '20250507165750-825', '20250507165750-825', 1),
                                                                                                                    ('UA10T-TEM-24060879', 'temp', null, null, null, '20250507165750-825', '20250507165750-825', 1);

# 탈휘 설비(설비)
INSERT INTO sensor_info (sensor_id, sensor_type, val_unit, sensor_thres, created_at, zone_id, equip_id, iszone) VALUES
                                                                                                                    ('UA10P-APWR-24060878', 'active_power', null, null, null, '20250507165750-825', '20250507171316-387', 0),
                                                                                                                    ('UA10P-RPWR-24060878', 'reactive_power', null, null, null, '20250507165750-825', '20250507171316-387', 0),
                                                                                                                    ('UA10H-HUM-24060878', 'humid', null, null, null, '20250507165750-825', '20250507171316-387', 0),
                                                                                                                    ('UA10H-PRS-34060878', 'pressure', null, null, null, '20250507165750-825', '20250507171316-387', 0),
                                                                                                                    ('UA10T-TEM-24060878', 'temp', null, null, null, '20250507165750-825', '20250507171316-387', 0),
                                                                                                                    ('UA10V-VIB-24060878', 'vibration', null, null, null, '20250507165750-825', '20250507171316-387', 0);