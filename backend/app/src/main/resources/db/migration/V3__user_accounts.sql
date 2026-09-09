create table app_user (
    id           bigint generated always as identity primary key,
    created_by   varchar(100) not null default 'system',
    created_when timestamptz  not null default now(),
    changed_by   varchar(100) not null default 'system',
    changed_when timestamptz  not null default now(),
    version      bigint       not null default 0,
    email         varchar(320) not null,
    password_hash varchar(200) not null,
    display_name  varchar(100) not null,
    role          varchar(20)  not null check (role in ('USER', 'ADMIN')),
    status        varchar(30)  not null check (status in ('ACTIVE', 'BLOCKED', 'PENDING_VERIFICATION'))
);

create unique index uq_app_user_email on app_user (lower(email));

create table user_token (
    id           bigint generated always as identity primary key,
    created_by   varchar(100) not null default 'system',
    created_when timestamptz  not null default now(),
    changed_by   varchar(100) not null default 'system',
    changed_when timestamptz  not null default now(),
    version      bigint       not null default 0,
    user_id     bigint       not null references app_user (id),
    token_type  varchar(30)  not null check (token_type in ('VERIFY_EMAIL', 'RESET_PASSWORD')),
    token_hash  varchar(200) not null,
    expires_at  timestamptz  not null,
    consumed_at timestamptz
);

create unique index uq_user_token_hash on user_token (token_hash);
create index ix_user_token_user on user_token (user_id);
