create table portfolio (
    id           bigint generated always as identity primary key,
    created_by   varchar(100) not null default 'system',
    created_when timestamptz  not null default now(),
    changed_by   varchar(100) not null default 'system',
    changed_when timestamptz  not null default now(),
    version      bigint       not null default 0,
    user_id  bigint       not null references app_user (id),
    name     varchar(100) not null,
    archived boolean      not null default false
);

create index ix_portfolio_user on portfolio (user_id);

create table holding (
    id           bigint generated always as identity primary key,
    created_by   varchar(100) not null default 'system',
    created_when timestamptz  not null default now(),
    changed_by   varchar(100) not null default 'system',
    changed_when timestamptz  not null default now(),
    version      bigint       not null default 0,
    portfolio_id  bigint         not null references portfolio (id),
    instrument_id bigint         not null references instrument (id),
    quantity      numeric(38, 18) not null default 0,
    avg_cost      numeric(38, 18) not null default 0
);

create unique index uq_holding_portfolio_instrument on holding (portfolio_id, instrument_id);
create index ix_holding_instrument on holding (instrument_id);

create table trade (
    id           bigint generated always as identity primary key,
    created_by   varchar(100) not null default 'system',
    created_when timestamptz  not null default now(),
    changed_by   varchar(100) not null default 'system',
    changed_when timestamptz  not null default now(),
    version      bigint       not null default 0,
    portfolio_id  bigint         not null references portfolio (id),
    instrument_id bigint         not null references instrument (id),
    side          varchar(10)    not null check (side in ('BUY', 'SELL')),
    quantity      numeric(38, 18) not null,
    price         numeric(38, 18) not null,
    gross_amount  numeric(20, 2) not null,
    executed_at   timestamptz    not null,
    flagged       boolean        not null default false
);

create index ix_trade_portfolio on trade (portfolio_id);
create index ix_trade_instrument on trade (instrument_id);
create index ix_trade_executed_at on trade (executed_at);
