alter table portfolio
    add constraint uq_portfolio_id_user unique (id, user_id);

create unique index uq_portfolio_user_name on portfolio (user_id, lower(name));

alter table wallet
    add column portfolio_id bigint,
    add constraint wallet_portfolio_owner_fkey foreign key (portfolio_id, user_id) references portfolio (id, user_id),
    add constraint wallet_balance_check check (cash_balance >= 0 and reserved_balance >= 0 and reserved_balance <= cash_balance),
    alter column currency drop default;

drop index uq_wallet_user;
create unique index uq_wallet_user_main on wallet (user_id) where portfolio_id is null;
create unique index uq_wallet_portfolio on wallet (portfolio_id);

alter table wallet_entry
    drop constraint wallet_entry_entry_type_check,
    add constraint wallet_entry_entry_type_check check (entry_type in
        ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_IN', 'TRANSFER_OUT', 'TRADE_DEBIT', 'TRADE_CREDIT', 'FEE',
         'RESERVE', 'RELEASE', 'INCOME')),
    add column effective_at timestamptz not null default now();

drop index ix_wallet_entry_wallet;
create index ix_wallet_entry_wallet_effective on wallet_entry (wallet_id, effective_at);

create table portfolio_transfer (
    id           bigint generated always as identity primary key,
    created_by   varchar(100) not null default 'system',
    created_when timestamptz  not null default now(),
    changed_by   varchar(100) not null default 'system',
    changed_when timestamptz  not null default now(),
    version      bigint       not null default 0,
    portfolio_id   bigint          not null references portfolio (id),
    direction      varchar(10)     not null check (direction in ('IN', 'OUT')),
    amount         numeric(20, 2)  not null check (amount > 0),
    main_amount    numeric(20, 2)  not null check (main_amount > 0),
    fx_rate        numeric(38, 18) check (fx_rate > 0),
    fx_fee_amount  numeric(20, 2)  not null default 0 check (fx_fee_amount >= 0),
    transferred_at timestamptz     not null,
    constraint portfolio_transfer_fx_check
        check (fx_rate is not null or (main_amount = amount and fx_fee_amount = 0))
);

create index ix_portfolio_transfer_portfolio on portfolio_transfer (portfolio_id, transferred_at);

select history_enable(t) from unnest(array['wallet', 'wallet_entry', 'portfolio_transfer']) as t;

select set_config('app.actor', 'system', true);

update wallet w
   set currency = 'PLN'
 where w.portfolio_id is null
   and w.currency <> 'PLN'
   and w.cash_balance = 0
   and w.reserved_balance = 0
   and not exists (select 1 from wallet_entry e where e.wallet_id = w.id);

with granted as (
    update wallet w
       set cash_balance = 1000.00
      from app_user u
     where u.id = w.user_id
       and u.status = 'ACTIVE'
       and w.portfolio_id is null
       and w.currency = 'PLN'
       and w.cash_balance = 0
       and not exists (select 1 from wallet_entry e where e.wallet_id = w.id)
    returning w.id, w.cash_balance
)
insert into wallet_entry (wallet_id, entry_type, amount, balance_after, reference_type, effective_at)
select id, 'DEPOSIT', cash_balance, cash_balance, 'STARTING_BALANCE', now() from granted;
