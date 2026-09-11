alter index idx_process_instance_status rename to ix_process_instance_status;
alter index idx_process_instance_definition rename to ix_process_instance_definition_key;
drop index idx_process_step_log_instance;

alter table process_instance
    add constraint process_instance_status_check
        check (status in ('RUNNING', 'WAITING', 'COMPLETED', 'FAILED'));

alter table process_step_log
    add constraint process_step_log_disposition_check
        check (disposition in ('ADVANCE', 'AWAIT_INPUT', 'FAILED', 'REWIND'));

alter table wallet alter column currency type varchar(3);
alter table instrument alter column quote_currency type varchar(3);
alter table income_event alter column currency type varchar(3);
