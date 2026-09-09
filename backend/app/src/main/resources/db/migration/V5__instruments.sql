create table instrument (
    id           bigint generated always as identity primary key,
    created_by   varchar(100) not null default 'system',
    created_when timestamptz  not null default now(),
    changed_by   varchar(100) not null default 'system',
    changed_when timestamptz  not null default now(),
    version      bigint       not null default 0,
    symbol         varchar(30)  not null,
    display_name   varchar(100) not null,
    kind           varchar(20)  not null check (kind in ('CRYPTO', 'STOCK')),
    quote_currency char(3)      not null default 'USD',
    active         boolean      not null default true
);

create unique index uq_instrument_symbol on instrument (symbol);

create table instrument_quote (
    id           bigint generated always as identity primary key,
    created_by   varchar(100) not null default 'system',
    created_when timestamptz  not null default now(),
    changed_by   varchar(100) not null default 'system',
    changed_when timestamptz  not null default now(),
    version      bigint       not null default 0,
    instrument_id bigint         not null references instrument (id),
    price         numeric(38, 18) not null,
    change_24h    numeric(38, 18),
    high_24h      numeric(38, 18),
    low_24h       numeric(38, 18),
    volume_24h    numeric(38, 18),
    as_of         timestamptz    not null
);

create unique index uq_instrument_quote_instrument on instrument_quote (instrument_id);

create table price_candle (
    id           bigint generated always as identity primary key,
    created_by   varchar(100) not null default 'system',
    created_when timestamptz  not null default now(),
    changed_by   varchar(100) not null default 'system',
    changed_when timestamptz  not null default now(),
    version      bigint       not null default 0,
    instrument_id bigint         not null references instrument (id),
    interval_code varchar(10)    not null check (interval_code in ('M1', 'M5', 'H1', 'D1')),
    open_time     timestamptz    not null,
    open_price    numeric(38, 18) not null,
    high_price    numeric(38, 18) not null,
    low_price     numeric(38, 18) not null,
    close_price   numeric(38, 18) not null,
    volume        numeric(38, 18) not null,
    close_time    timestamptz    not null
);

create unique index uq_price_candle_slot on price_candle (instrument_id, interval_code, open_time);
create index ix_price_candle_instrument on price_candle (instrument_id);
