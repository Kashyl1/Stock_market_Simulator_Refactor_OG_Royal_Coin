create function history_touch() returns trigger
language plpgsql as $$
begin
    if OLD is not distinct from NEW then
        return NEW;
    end if;
    if NEW.changed_when is not distinct from OLD.changed_when then
        NEW.changed_when := greatest(now(), OLD.changed_when);
        NEW.changed_by := coalesce(nullif(current_setting('app.actor', true), ''), 'unknown');
    end if;
    return NEW;
end $$;

create function history_enable(p_table text) returns void
language plpgsql as $$
declare
    history_table     text := p_table || '_h';
    capture_function  text := p_table || '_history_capture';
    actor             text := 'coalesce(nullif(current_setting(''app.actor'', true), ''''), ''unknown'')';
    base_column       record;
    column_list       text;
    old_values        text;
    has_changed       boolean;
    has_updated       boolean;
    update_valid_to   text;
    update_changed_by text;
    delete_valid_to   text;
begin
    if to_regclass(history_table) is null then
        execute format('create table %I ('
                       '    h_id         bigint generated always as identity primary key,'
                       '    h_operation  varchar(6)   not null check (h_operation in (''UPDATE'', ''DELETE'')),'
                       '    h_valid_to   timestamptz  not null,'
                       '    h_changed_by varchar(100) not null,'
                       '    like %I'
                       ')', history_table, p_table);
        for base_column in
            select attname from pg_attribute
            where attrelid = p_table::regclass and attnum > 0 and not attisdropped
        loop
            execute format('alter table %I alter column %I drop not null', history_table, base_column.attname);
        end loop;
        execute format('create index %I on %I (id, h_valid_to)', 'ix_' || history_table || '_id', history_table);
    end if;

    for base_column in
        select b.attname, format_type(b.atttypid, b.atttypmod) as column_type
        from pg_attribute b
        where b.attrelid = p_table::regclass
          and b.attnum > 0
          and not b.attisdropped
          and not exists (select 1 from pg_attribute h
                          where h.attrelid = history_table::regclass and h.attname = b.attname and not h.attisdropped)
        order by b.attnum
    loop
        execute format('alter table %I add column %I %s', history_table, base_column.attname, base_column.column_type);
    end loop;

    select string_agg(quote_ident(attname), ', ' order by attnum),
           string_agg('OLD.' || quote_ident(attname), ', ' order by attnum)
    into column_list, old_values
    from pg_attribute
    where attrelid = p_table::regclass and attnum > 0 and not attisdropped;

    select count(*) = 2 into has_changed
    from pg_attribute
    where attrelid = p_table::regclass and attname in ('changed_when', 'changed_by') and not attisdropped;

    select count(*) = 1 into has_updated
    from pg_attribute
    where attrelid = p_table::regclass and attname = 'updated_at' and not attisdropped;

    update_valid_to := case when has_changed then 'NEW.changed_when' when has_updated then 'NEW.updated_at' else 'now()' end;
    update_changed_by := case when has_changed then 'NEW.changed_by' else actor end;
    delete_valid_to := case when has_changed then 'greatest(now(), OLD.changed_when)'
                            when has_updated then 'greatest(now(), OLD.updated_at)'
                            else 'now()' end;

    execute format($function$
        create or replace function %1$I() returns trigger
        language plpgsql as $body$
        begin
            if TG_OP = 'UPDATE' then
                if OLD is not distinct from NEW then
                    return null;
                end if;
                insert into %2$I (h_operation, h_valid_to, h_changed_by, %3$s)
                values ('UPDATE', %5$s, %6$s, %4$s);
            else
                insert into %2$I (h_operation, h_valid_to, h_changed_by, %3$s)
                values ('DELETE', %7$s, %8$s, %4$s);
            end if;
            return null;
        end $body$
    $function$, capture_function, history_table, column_list, old_values,
       update_valid_to, update_changed_by, delete_valid_to, actor);

    if has_changed then
        execute format('create or replace trigger %I before update on %I for each row execute function history_touch()',
                       p_table || '_history_touch', p_table);
    end if;
    execute format('create or replace trigger %I after update or delete on %I for each row execute function %I()',
                   p_table || '_history', p_table, capture_function);
end $$;

create view history_column_drift as
select base.relname as table_name, col.attname as column_name
from pg_class hist
join pg_class base on hist.relname = base.relname || '_h' and hist.relnamespace = base.relnamespace
join pg_attribute col on col.attrelid = base.oid and col.attnum > 0 and not col.attisdropped
where base.relkind = 'r'
  and not exists (select 1 from pg_attribute h
                  where h.attrelid = hist.oid and h.attname = col.attname and not h.attisdropped);

select history_enable(t) from unnest(array[
    'app_user',
    'user_token',
    'refresh_token',
    'wallet',
    'wallet_entry',
    'instrument',
    'portfolio',
    'holding',
    'trade',
    'income_event',
    'process_instance',
    'process_step_log'
]) as t;
