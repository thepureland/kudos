--region DDL
create table if not exists "sys_data_source"
(
    "id"                  char(36)  default RANDOM_UUID() not null primary key,
    "name"                character varying(64)           not null,
    "sub_system_code"     character varying(32)           not null,
    "micro_service_code"  character varying(32),
    "atomic_service_code" character varying(32),
    "tenant_id"           char(36),
    "url"                 character varying(256)          not null,
    "username"            character varying(32)           not null,
    "password"            character varying(256),
    "initial_size"        int2,
    "max_active"          int2,
    "max_idle"            int2,
    "min_idle"            int2,
    "max_wait"            int2,
    "max_age"             int2,
    "remark"              character varying(128),
    "active"              boolean   default TRUE          not null,
    "built_in"            boolean   default FALSE         not null,
    "create_user"         character varying(36),
    "create_time"         timestamp default now()         not null,
    "update_user"         character varying(36),
    "update_time"         timestamp
);

comment on table "sys_data_source" is '数据源';
comment on column "sys_data_source"."id" is '主键';
comment on column "sys_data_source"."name" is '名称';
comment on column "sys_data_source"."sub_system_code" is '子系统编码';
comment on column "sys_data_source"."micro_service_code" is '微服务编码';
comment on column "sys_data_source"."atomic_service_code" is '原子服务编码';
comment on column "sys_data_source"."tenant_id" is '租户id';
comment on column "sys_data_source"."url" is 'url';
comment on column "sys_data_source"."username" is '用户名';
comment on column "sys_data_source"."password" is '密码';
comment on column "sys_data_source"."initial_size" is '初始连接数。初始化发生在显示调用init方法，或者第一次getConnection时';
comment on column "sys_data_source"."max_active" is '最大连接数';
comment on column "sys_data_source"."max_idle" is '最大空闲连接数';
comment on column "sys_data_source"."min_idle" is '最小空闲连接数。至少维持多少个空闲连接';
comment on column "sys_data_source"."max_wait" is '出借最长期限(毫秒)。客户端从连接池获取（借出）一个连接后，超时没有归还（return），则连接池会抛出异常';
comment on column "sys_data_source"."max_age" is '连接寿命(毫秒)。超时(相对于初始化时间)连接池将在出借或归还时删除这个连接';
comment on column "sys_data_source"."remark" is '备注';
comment on column "sys_data_source"."active" is '是否启用';
comment on column "sys_data_source"."built_in" is '是否内置';
comment on column "sys_data_source"."create_user" is '创建用户';
comment on column "sys_data_source"."create_time" is '创建时间';
comment on column "sys_data_source"."update_user" is '更新用户';
comment on column "sys_data_source"."update_time" is '更新时间';
--endregion DDL


