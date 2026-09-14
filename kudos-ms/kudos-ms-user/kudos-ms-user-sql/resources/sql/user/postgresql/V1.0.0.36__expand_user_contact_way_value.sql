alter table "user_contact_way"
    alter column "contact_way_value" type varchar(254);

comment on column "user_contact_way"."contact_way_value" is
    '联系方式值；长度支持 RFC 邮箱地址上限';
