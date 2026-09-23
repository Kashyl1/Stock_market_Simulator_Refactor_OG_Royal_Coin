update app_user set email = lower(email);

alter table app_user
    add constraint app_user_email_lowercase_check check (email = lower(email));
