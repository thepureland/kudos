create table if not exists "sys_cache"
(
    "id"                 CHAR(36)  default RANDOM_UUID() not null primary key,
    "name"               VARCHAR(64)                     not null,
    "sub_sys_dict_code"  VARCHAR(32)                     not null,
    "strategy_dict_code" VARCHAR(16)                     not null,
    "write_on_boot"      BOOLEAN   default FALSE         not null,
    "write_in_time"      BOOLEAN   default FALSE         not null,
    "ttl"                INT2,
    "remark"             VARCHAR(128),
    "active"             BOOLEAN   default TRUE          not null,
    "built_in"           BOOLEAN   default FALSE         not null,
    "create_user"        VARCHAR(36),
    "create_time"        TIMESTAMP default now(),
    "update_user"        VARCHAR(36),
    "update_time"        TIMESTAMP
);

create unique index if not exists "uq_sys_cache" on "sys_cache" ("name");

comment on table "sys_cache" is '缓存';
comment on column "sys_cache"."id" is '主键';
comment on column "sys_cache"."name" is '名称';
comment on column "sys_cache"."sub_sys_dict_code" is '子系统代码';
comment on column "sys_cache"."strategy_dict_code" is '缓存策略代码';
comment on column "sys_cache"."write_on_boot" is '是否启动时写缓存';
comment on column "sys_cache"."write_in_time" is '是否及时回写缓存';
comment on column "sys_cache"."ttl" is '缓存生存时间(秒)';
comment on column "sys_cache"."remark" is '备注，或其国际化key';
comment on column "sys_cache"."active" is '是否启用';
comment on column "sys_cache"."built_in" is '是否内置';
comment on column "sys_cache"."create_user" is '创建用户';
comment on column "sys_cache"."create_time" is '创建时间';
comment on column "sys_cache"."update_user" is '更新用户';
comment on column "sys_cache"."update_time" is '更新时间';



create table if not exists "sys_dict"
(
    "id"          character varying(36) default RANDOM_UUID() not null primary key,
    "dict_module" character varying(32)                       not null,
    "dict_type"   character varying(64)                       not null,
    "dict_name"   character varying(64)                       not null,
    "remark"      character varying(300),
    "active"      boolean               default TRUE          not null,
    "built_in"    boolean               default FALSE         not null
);

create index if not exists "idx_sys_dict_dict_module" on "sys_dict" ("dict_module");
create unique index if not exists "uk_sys_dict_dict_module_dict_type" on "sys_dict" ("dict_module", "dict_type");

comment on table "sys_dict" is '字典';
comment on column "sys_dict"."id" is '主键id';
comment on column "sys_dict"."dict_type" is '字典类型';
comment on column "sys_dict"."dict_name" is '字典名称';
comment on column "sys_dict"."remark" is '备注';
comment on column "sys_dict"."active" is '是否启用';
comment on column "sys_dict"."built_in" is '是否内置';




create table if not exists "sys_dict_item"
(
     "id"        VARCHAR(36) DEFAULT RANDOM_UUID() NOT NULL PRIMARY KEY,
     "dict_id"   VARCHAR(36)                       NOT NULL,
     "item_code" VARCHAR(64)                       NOT NULL,
     "item_name" VARCHAR(64)                       NOT NULL,
     "order_num" INTEGER,
     "remark"    VARCHAR(300),
     "active"    BOOLEAN   DEFAULT TRUE  NOT NULL,
     "built_in"  BOOLEAN   DEFAULT FALSE NOT NULL
);

create index if not exists "fk_sys_dict_item_dict_id" on "sys_dict_item" ("dict_id");
create unique index if not exists "uk_sys_dict_item_dict_id_item_code" on "sys_dict_item" ("dict_id", "item_code");

comment on table "sys_dict_item" is '字典项';
comment on column "sys_dict_item"."id" is '主键id';
comment on column "sys_dict_item"."dict_id" is '字典id';
comment on column "sys_dict_item"."item_code" is '字典项代码';
comment on column "sys_dict_item"."item_name" is '字典项名称';
comment on column "sys_dict_item"."order_num" is '字典项排序';
comment on column "sys_dict_item"."remark" is '备注';
comment on column "sys_dict_item"."active" is '是否启用';
comment on column "sys_dict_item"."built_in" is '是否内置';



create table if not exists "sys_dict_item_i18n"
(
    "id"         character varying(36) default RANDOM_UUID() not null primary key,
    "item_id"    character varying(36)                       not null,
    "locale"     character varying(8)                        not null,
    "i18n_value" character varying(1000)                     not null,
    "trans"      boolean               default FALSE         not null
);

create index if not exists "fk_sys_dict_item_i18n_item_id" on "sys_dict_item_i18n" ("item_id");
create unique index if not exists "uk_sys_dict_item_i18n_item_id_locale" on "sys_dict_item_i18n" ("item_id", "locale");

comment on table "sys_dict_item_i18n" is '字典项国际化';
comment on column "sys_dict_item_i18n"."id" is '主键id';
comment on column "sys_dict_item_i18n"."item_id" is '字典项id';
comment on column "sys_dict_item_i18n"."locale" is '语言_地区';
comment on column "sys_dict_item_i18n"."i18n_value" is '国际化值';
comment on column "sys_dict_item_i18n"."trans" is '是否翻译';



create table if not exists "sys_resource"
(
    "id"                      CHAR(36)  default RANDOM_UUID() not null primary key,
    "name"                    VARCHAR(64)                     not null,
    "url"                     VARCHAR(128),
    "resource_type_dict_code" CHAR(1)                         not null,
    "parent_id"               CHAR(36),
    "seq_no"                  INT2,
    "sub_sys_dict_code"       VARCHAR(32)                     not null,
    "icon"                    VARCHAR(128),
    "remark"                  VARCHAR(128),
    "active"                  BOOLEAN   default TRUE          not null,
    "built_in"                BOOLEAN   default FALSE         not null,
    "create_user"             VARCHAR(36),
    "create_time"             TIMESTAMP default now()         not null,
    "update_user"             VARCHAR(36),
    "update_time"             TIMESTAMP
);

create unique index if not exists "uq_sys_resource__name_sub_sys"  on "sys_resource" ("name", "sub_sys_dict_code");

comment on table "sys_resource" is '资源';
comment on column "sys_resource"."id" is '主键';
comment on column "sys_resource"."name" is '名称，或其国际化key';
comment on column "sys_resource"."url" is 'url';
comment on column "sys_resource"."resource_type_dict_code" is '资源类型字典代码';
comment on column "sys_resource"."parent_id" is '父id';
comment on column "sys_resource"."seq_no" is '在同父节点下的排序号';
comment on column "sys_resource"."sub_sys_dict_code" is '子系统代码';
comment on column "sys_resource"."icon" is '图标';
comment on column "sys_resource"."remark" is '备注，或其国际化key';
comment on column "sys_resource"."active" is '是否启用';
comment on column "sys_resource"."built_in" is '是否内置';
comment on column "sys_resource"."create_user" is '创建用户';
comment on column "sys_resource"."create_time" is '创建时间';
comment on column "sys_resource"."update_user" is '更新用户';
comment on column "sys_resource"."update_time" is '更新时间';



create table if not exists "sys_datasource"
(
    "id"                VARCHAR(36) default RANDOM_UUID() not null primary key,
    "name"              VARCHAR(32)                       not null,
    "sub_sys_dict_code" VARCHAR(32)                       not null,
    "tenant_id"         VARCHAR(36),
    "url"               VARCHAR(256)                      not null,
    "username"          VARCHAR(32)                       not null,
    "password"          VARCHAR(128),
    "initial_size"      INT2,
    "max_active"        INT2,
    "max_idle"          INT2,
    "min_idle"          INT2,
    "max_wait"          INT2,
    "max_age"           INT2,
    "remark"            VARCHAR(128),
    "active"            BOOLEAN     default TRUE          not null,
    "built_in"          BOOLEAN     default FALSE         not null,
    "create_user"       VARCHAR(36),
    "create_time"       TIMESTAMP   default now()         not null,
    "update_user"       VARCHAR(36),
    "update_time"       TIMESTAMP
);

comment on table "sys_datasource" is '数据源';
comment on column "sys_datasource"."id" is '主键';
comment on column "sys_datasource"."name" is '名称，或其国际化key';
comment on column "sys_datasource"."sub_sys_dict_code" is '子系统代码';
comment on column "sys_datasource"."tenant_id" is '租户id';
comment on column "sys_datasource"."url" is 'url';
comment on column "sys_datasource"."username" is '用户名';
comment on column "sys_datasource"."password" is '密码，强烈建议加密';
comment on column "sys_datasource"."initial_size" is '初始连接数。初始化发生在显示调用init方法，或者第一次getConnection时';
comment on column "sys_datasource"."max_active" is '最大连接数';
comment on column "sys_datasource"."max_idle" is '最大空闲连接数';
comment on column "sys_datasource"."min_idle" is '最小空闲连接数。至少维持多少个空闲连接';
comment on column "sys_datasource"."max_wait" is '出借最长期限(毫秒)。客户端从连接池获取（借出）一个连接后，超时没有归还（return），则连接池会抛出异常';
comment on column "sys_datasource"."max_age" is '连接寿命(毫秒)。超时(相对于初始化时间)连接池将在出借或归还时删除这个连接';
comment on column "sys_datasource"."remark" is '备注，或其国际化key';
comment on column "sys_datasource"."active" is '是否启用';
comment on column "sys_datasource"."built_in" is '是否内置';
comment on column "sys_datasource"."create_user" is '创建用户';
comment on column "sys_datasource"."create_time" is '创建时间';
comment on column "sys_datasource"."update_user" is '更新用户';
comment on column "sys_datasource"."update_time" is '更新时间';


create table if not exists "sys_tenant"
(
    "id"                CHAR(36)  default RANDOM_UUID() not null primary key,
    "sub_sys_dict_code" VARCHAR(32)                     not null,
    "name"              VARCHAR(64)                     not null,
    "remark"            VARCHAR(128),
    "active"            BOOLEAN   default TRUE          not null,
    "built_in"          BOOLEAN   default FALSE         not null,
    "create_user"       VARCHAR(36),
    "create_time"       TIMESTAMP default now(),
    "update_user"       VARCHAR(36),
    "update_time"       TIMESTAMP
);

create unique index if not exists "uq_sys_tenant" on "sys_tenant" ("sub_sys_dict_code", "name");

comment on table "sys_tenant" is '租户';
comment on column "sys_tenant"."id" is '主键';
comment on column "sys_tenant"."sub_sys_dict_code" is '子系统代码';
comment on column "sys_tenant"."name" is '名称';
comment on column "sys_tenant"."remark" is '备注，或其国际化key';
comment on column "sys_tenant"."active" is '是否启用';
comment on column "sys_tenant"."built_in" is '是否内置';
comment on column "sys_tenant"."create_user" is '创建用户';
comment on column "sys_tenant"."create_time" is '创建时间';
comment on column "sys_tenant"."update_user" is '更新用户';
comment on column "sys_tenant"."update_time" is '更新时间';


create table if not exists "sys_domain"
(
    "id"                VARCHAR(36) default RANDOM_UUID() not null primary key,
    "domain"            VARCHAR(64)                       not null,
    "sub_sys_dict_code" VARCHAR(32)                       not null,
    "tenant_id"         VARCHAR(36),
    "remark"            VARCHAR(128),
    "active"            BOOLEAN     default TRUE          not null,
    "built_in"          BOOLEAN     default FALSE         not null,
    "create_user"       VARCHAR(36),
    "create_time"       TIMESTAMP   default now()         not null,
    "update_user"       VARCHAR(36),
    "update_time"       TIMESTAMP
);

create unique index if not exists "uq_sys_domain" on "sys_domain" ("domain");

comment on table "sys_domain" is '域名';
comment on column "sys_domain"."id" is '主键';
comment on column "sys_domain"."domain" is '域名';
comment on column "sys_domain"."sub_sys_dict_code" is '子系统代码';
comment on column "sys_domain"."tenant_id" is '租户id';
comment on column "sys_domain"."remark" is '备注，或其国际化key';
comment on column "sys_domain"."active" is '是否启用';
comment on column "sys_domain"."built_in" is '是否内置';
comment on column "sys_domain"."create_user" is '创建用户';
comment on column "sys_domain"."create_time" is '创建时间';
comment on column "sys_domain"."update_user" is '更新用户';
comment on column "sys_domain"."update_time" is '更新时间';


create table if not exists "sys_param"
(
    "id"            CHAR(36)  default RANDOM_UUID() not null primary key,
    "module"        VARCHAR(64)                     not null,
    "param_name"    VARCHAR(32)                     not null,
    "param_value"   VARCHAR(128)                    not null,
    "default_value" VARCHAR(128),
    "seq_no"        INT2,
    "remark"        VARCHAR(128),
    "active"        BOOLEAN   default TRUE          not null,
    "built_in"      BOOLEAN   default FALSE         not null,
    "create_user"   VARCHAR(36),
    "create_time"   TIMESTAMP default now()         not null,
    "update_user"   VARCHAR(36),
    "update_time"   TIMESTAMP
);

create unique index if not exists "uq_sys_param__param_name_module" on "sys_param" ("param_name", "module");

comment on table "sys_param" is '参数';
comment on column "sys_param"."id" is '主键';
comment on column "sys_param"."module" is '模块';
comment on column "sys_param"."param_name" is '参数名称';
comment on column "sys_param"."param_value" is '参数值，或其国际化key';
comment on column "sys_param"."default_value" is '默认参数值，或其国际化key';
comment on column "sys_param"."seq_no" is '序号';
comment on column "sys_param"."remark" is '备注，或其国际化key';
comment on column "sys_param"."active" is '是否启用';
comment on column "sys_param"."built_in" is '是否内置';
comment on column "sys_param"."create_user" is '创建用户';
comment on column "sys_param"."create_time" is '创建时间';
comment on column "sys_param"."update_user" is '更新用户';
comment on column "sys_param"."update_time" is '更新时间';

