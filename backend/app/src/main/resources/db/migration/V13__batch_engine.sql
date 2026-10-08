create table batch_type (
    id           bigint generated always as identity primary key,
    created_by   varchar(100) not null default 'system',
    created_when timestamptz  not null default now(),
    changed_by   varchar(100) not null default 'system',
    changed_when timestamptz  not null default now(),
    version      bigint       not null default 0,
    code        varchar(50)  not null,
    name        varchar(100) not null,
    description varchar(500) not null,
    cron        varchar(100),
    zone        varchar(50)  not null,
    enabled     boolean      not null default true,
    constraint uq_batch_type_code unique (code)
);

insert into batch_type (code, name, description, cron, zone) values
    ('FX_RATES_NBP', 'FX rates from NBP', 'Stores the NBP table A mid rate of every active currency against PLN for every day published from the newest stored day on', '0 30 12,16 * * MON-FRI', 'Europe/Warsaw');

create table batch_job (
    id           bigint generated always as identity primary key,
    created_by   varchar(100) not null default 'system',
    created_when timestamptz  not null default now(),
    changed_by   varchar(100) not null default 'system',
    changed_when timestamptz  not null default now(),
    version      bigint       not null default 0,
    batch_type_id     bigint       not null references batch_type (id),
    status            varchar(30)  not null check (status in ('SCHEDULED', 'RUNNING', 'COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAILED', 'STOPPED')),
    origin            varchar(10)  not null check (origin in ('CRON', 'MANUAL')),
    scheduled_for     timestamptz  not null,
    started_at        timestamptz,
    finished_at       timestamptz,
    total_items       integer      check (total_items >= 0),
    succeeded_items   integer      not null default 0 check (succeeded_items >= 0),
    failed_items      integer      not null default 0 check (failed_items >= 0),
    stop_requested_by varchar(100),
    stop_requested_at timestamptz,
    error_code        varchar(100),
    error_message     varchar(500),
    acknowledged_by   varchar(100),
    acknowledged_at   timestamptz
);

create unique index uq_batch_job_running_per_type on batch_job (batch_type_id) where status = 'RUNNING';
create unique index uq_batch_job_cron_scheduled_per_type on batch_job (batch_type_id) where status = 'SCHEDULED' and origin = 'CRON';
create index ix_batch_job_status_scheduled_for on batch_job (status, scheduled_for);
create index ix_batch_job_type_scheduled_for on batch_job (batch_type_id, scheduled_for);

create table batch_item (
    id           bigint generated always as identity primary key,
    created_by   varchar(100) not null default 'system',
    created_when timestamptz  not null default now(),
    changed_by   varchar(100) not null default 'system',
    changed_when timestamptz  not null default now(),
    version      bigint       not null default 0,
    batch_job_id  bigint       not null references batch_job (id),
    item_key      varchar(100) not null,
    status        varchar(20)  not null check (status in ('SUCCEEDED', 'FAILED')),
    payload       text,
    error_code    varchar(100),
    error_message varchar(500),
    processed_at  timestamptz  not null
);

create index ix_batch_item_job_status on batch_item (batch_job_id, status);

select history_enable(t) from unnest(array['batch_type', 'batch_job']) as t;
