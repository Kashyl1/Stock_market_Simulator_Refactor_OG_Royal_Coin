create table refresh_token (
    id           bigint generated always as identity primary key,
    created_by   varchar(100) not null default 'system',
    created_when timestamptz  not null default now(),
    changed_by   varchar(100) not null default 'system',
    changed_when timestamptz  not null default now(),
    version      bigint       not null default 0,
    user_id     bigint       not null references app_user (id),
    token_hash  varchar(200) not null,
    family_id   uuid         not null,
    issued_at   timestamptz  not null,
    expires_at  timestamptz  not null,
    revoked_at  timestamptz,
    replaced_by bigint,
    user_agent  varchar(400),
    ip          varchar(64)
);

create unique index uq_refresh_token_hash on refresh_token (token_hash);
create index ix_refresh_token_user on refresh_token (user_id);
create index ix_refresh_token_family on refresh_token (family_id);
