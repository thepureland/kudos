-- IANA zone ids such as America/Argentina/Buenos_Aires do not fit the historical varchar(16).
alter table "user_account" alter column "default_timezone" type varchar(64);
