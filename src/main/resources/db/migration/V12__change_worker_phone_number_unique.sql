ALTER TABLE worker_info
    ADD CONSTRAINT uc_worker_info_phone_number UNIQUE (phone_number);