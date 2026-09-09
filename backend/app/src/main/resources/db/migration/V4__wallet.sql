create table wallet (
    id           bigint generated always as identity primary key,
    created_by   varchar(100) not null default 'system',
    created_when timestamptz  not null default now(),
    changed_by   varchar(100) not null default 'system',
    changed_when timestamptz  not null default now(),
    version      bigint       not null default 0,
    user_id          bigint        not null references app_user (id),
    currency         char(3)       not null default 'USD',
    cash_balance     numeric(20, 2) not null default 0,
    reserved_balance numeric(20, 2) not null default 0
);

create unique index uq_wallet_user on wallet (user_id);

create table wallet_entry (
    id           bigint generated always as identity primary key,
    created_by   varchar(100) not null default 'system',
    created_when timestamptz  not null default now(),
    changed_by   varchar(100) not null default 'system',
    changed_when timestamptz  not null default now(),
    version      bigint       not null default 0,
    wallet_id      bigint         not null references wallet (id),
    entry_type     varchar(30)    not null check (entry_type in
                       ('DEPOSIT', 'WITHDRAWAL', 'TRADE_DEBIT', 'TRADE_CREDIT', 'RESERVE', 'RELEASE')),
    amount         numeric(20, 2) not null,
    balance_after  numeric(20, 2) not null,
    reference_type varchar(40),
    reference_id   bigint
);

create index ix_wallet_entry_wallet on wallet_entry (wallet_id);
create index ix_wallet_entry_reference on wallet_entry (reference_type, reference_id);
