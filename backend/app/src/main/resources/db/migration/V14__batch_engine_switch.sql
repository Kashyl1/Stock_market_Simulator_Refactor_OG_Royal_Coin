create table batch_engine (
    id           bigint generated always as identity primary key,
    created_by   varchar(100) not null default 'system',
    created_when timestamptz  not null default now(),
    changed_by   varchar(100) not null default 'system',
    changed_when timestamptz  not null default now(),
    version      bigint       not null default 0,
    paused       boolean      not null default false
);

create unique index uq_batch_engine_single_row on batch_engine ((true));

insert into batch_engine (paused) values (false);

select history_enable('batch_engine');
