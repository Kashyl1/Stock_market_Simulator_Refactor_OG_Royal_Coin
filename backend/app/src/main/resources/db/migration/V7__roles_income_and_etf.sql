alter table app_user
    drop constraint app_user_role_check,
    add constraint app_user_role_check check (role in ('USER', 'SUPPORT', 'ADMIN'));

alter table instrument
    drop constraint instrument_kind_check,
    add constraint instrument_kind_check check (kind in ('CRYPTO', 'STOCK', 'ETF'));

alter table wallet_entry
    drop constraint wallet_entry_entry_type_check,
    add constraint wallet_entry_entry_type_check check (entry_type in
        ('DEPOSIT', 'WITHDRAWAL', 'TRADE_DEBIT', 'TRADE_CREDIT', 'RESERVE', 'RELEASE', 'INCOME'));

create table income_event (
    id           bigint generated always as identity primary key,
    created_by   varchar(100) not null default 'system',
    created_when timestamptz  not null default now(),
    changed_by   varchar(100) not null default 'system',
    changed_when timestamptz  not null default now(),
    version      bigint       not null default 0,
    portfolio_id     bigint         not null references portfolio (id),
    instrument_id    bigint         references instrument (id),
    income_type      varchar(30)    not null check (income_type in
                         ('DIVIDEND', 'STAKING_REWARD', 'INTEREST', 'CASH_ADJUSTMENT')),
    amount           numeric(20, 2) not null,
    currency         char(3)        not null default 'USD',
    received_at      timestamptz    not null,
    source_reference varchar(100)
);

create index ix_income_event_portfolio on income_event (portfolio_id);
create index ix_income_event_instrument on income_event (instrument_id);
create index ix_income_event_received_at on income_event (received_at);
