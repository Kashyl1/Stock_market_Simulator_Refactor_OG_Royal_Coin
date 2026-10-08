create table currency (
    id           bigint generated always as identity primary key,
    created_by   varchar(100) not null default 'system',
    created_when timestamptz  not null default now(),
    changed_by   varchar(100) not null default 'system',
    changed_when timestamptz  not null default now(),
    version      bigint       not null default 0,
    code        varchar(3)   not null check (code ~ '^[A-Z]{3}$'),
    name        varchar(100) not null,
    minor_units smallint     not null check (minor_units between 0 and 4),
    active      boolean      not null default true,
    constraint uq_currency_code unique (code)
);

insert into currency (code, name, minor_units) values
    ('PLN', 'Polish zloty', 2),
    ('USD', 'US dollar', 2),
    ('EUR', 'Euro', 2),
    ('GBP', 'Pound sterling', 2),
    ('CHF', 'Swiss franc', 2),
    ('JPY', 'Japanese yen', 0),
    ('CAD', 'Canadian dollar', 2),
    ('AUD', 'Australian dollar', 2),
    ('NZD', 'New Zealand dollar', 2),
    ('SEK', 'Swedish krona', 2),
    ('NOK', 'Norwegian krone', 2),
    ('DKK', 'Danish krone', 2),
    ('CZK', 'Czech koruna', 2),
    ('HUF', 'Hungarian forint', 2),
    ('RON', 'Romanian leu', 2),
    ('TRY', 'Turkish lira', 2),
    ('ILS', 'Israeli new shekel', 2),
    ('CNY', 'Chinese yuan renminbi', 2),
    ('HKD', 'Hong Kong dollar', 2),
    ('SGD', 'Singapore dollar', 2),
    ('KRW', 'South Korean won', 0),
    ('INR', 'Indian rupee', 2),
    ('MXN', 'Mexican peso', 2),
    ('BRL', 'Brazilian real', 2),
    ('ZAR', 'South African rand', 2),
    ('THB', 'Thai baht', 2),
    ('MYR', 'Malaysian ringgit', 2),
    ('IDR', 'Indonesian rupiah', 2),
    ('UAH', 'Ukrainian hryvnia', 2),
    ('ISK', 'Icelandic krona', 0);

create table fx_rate (
    id           bigint generated always as identity primary key,
    created_by   varchar(100) not null default 'system',
    created_when timestamptz  not null default now(),
    changed_by   varchar(100) not null default 'system',
    changed_when timestamptz  not null default now(),
    version      bigint       not null default 0,
    base_currency  varchar(3)      not null references currency (code),
    quote_currency varchar(3)      not null references currency (code),
    rate_date      date            not null,
    rate           numeric(38, 18) not null check (rate > 0),
    source         varchar(30)     not null,
    constraint fx_rate_pair_check check (base_currency <> quote_currency)
);

create unique index uq_fx_rate_pair_day on fx_rate (base_currency, quote_currency, rate_date);
create index ix_fx_rate_quote_currency on fx_rate (quote_currency);

alter table wallet
    add constraint wallet_currency_fkey foreign key (currency) references currency (code);

alter table instrument
    add constraint instrument_quote_currency_fkey foreign key (quote_currency) references currency (code);

alter table income_event
    add constraint income_event_currency_fkey foreign key (currency) references currency (code);

select history_enable(t) from unnest(array['currency', 'fx_rate']) as t;
