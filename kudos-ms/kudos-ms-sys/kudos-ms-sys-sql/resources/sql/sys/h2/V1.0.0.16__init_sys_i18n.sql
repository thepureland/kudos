--region DDL
create table if not exists "sys_i18n"
(
    "id"                  character(36) default RANDOM_UUID() not null primary key,
    "locale"              character(5)                not null,
    "atomic_service_code" character varying(32)               not null,
    "i18n_type_dict_code" character varying(32)               not null,
    "namespace"       character varying(128)             not null,
    "key"                 character varying(128)              not null,
    "value"               character varying(1000)             not null,
    "remark"              character varying(300),
    "active"              boolean       default TRUE          not null,
    "built_in"            boolean       default FALSE         not null,
    "create_user_id"      character varying(36),
    "create_user_name"    character varying(32),
    "create_time"         timestamp(6) default now()          not null,
    "update_user_id"      character varying(36),
    "update_user_name"    character varying(32),
    "update_time"         timestamp(6)
);

create unique index if not exists "uq_sys_i18n"
    on "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "namespace", "key");


comment on table "sys_i18n" is '国际化';
comment on column "sys_i18n"."id" is '主键';
comment on column "sys_i18n"."locale" is '语言_地区';
comment on column "sys_i18n"."atomic_service_code" is '原子服务编码';
comment on column "sys_i18n"."i18n_type_dict_code" is '国际化类型字典代码';
comment on column "sys_i18n"."namespace" is '国际化命名空间';
comment on column "sys_i18n"."key" is '国际化key';
comment on column "sys_i18n"."value" is '国际化值';
comment on column "sys_i18n"."remark" is '备注';
comment on column "sys_i18n"."active" is '是否启用';
comment on column "sys_i18n"."built_in" is '是否内置';
comment on column "sys_i18n"."create_user_id" is '创建者id';
comment on column "sys_i18n"."create_user_name" is '创建者名称';
comment on column "sys_i18n"."create_time" is '创建时间';
comment on column "sys_i18n"."update_user_id" is '更新者id';
comment on column "sys_i18n"."update_user_name" is '更新者名称';
comment on column "sys_i18n"."update_time" is '更新时间';
--endregion DDL

--region DML

-- dict
insert into "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "namespace", "key", "value", "built_in") values
    ('zh-CN', 'sys', 'dict', 'dict', 'i18n_type', '国际化类型', true),
    ('zh-TW', 'sys', 'dict', 'dict', 'i18n_type', '國際化類型', true),
    ('en-US', 'sys', 'dict', 'dict', 'i18n_type', 'I18N Type', true);


-- dict-item ds_use
insert into "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "namespace", "key", "value", "built_in") values
    ('zh-CN', 'sys', 'dict-item', 'ds_use', 'local', '本地数据源', true),
    ('zh-TW', 'sys', 'dict-item', 'ds_use', 'local', '本地資料來源', true),
    ('en-US', 'sys', 'dict-item', 'ds_use', 'local', 'Local Data Source', true),

    ('zh-CN', 'sys', 'dict-item', 'ds_use', 'remote', '远程数据源', true),
    ('zh-TW', 'sys', 'dict-item', 'ds_use', 'remote', '遠端資料來源', true),
    ('en-US', 'sys', 'dict-item', 'ds_use', 'remote', 'Remote Data Source', true),

    ('zh-CN', 'sys', 'dict-item', 'ds_use', 'report', '报表数据源', true),
    ('zh-TW', 'sys', 'dict-item', 'ds_use', 'report', '報表資料來源', true),
    ('en-US', 'sys', 'dict-item', 'ds_use', 'report', 'Report Data Source', true),

    ('zh-CN', 'sys', 'dict-item', 'ds_use', 'readonly', '只读数据源', true),
    ('zh-TW', 'sys', 'dict-item', 'ds_use', 'readonly', '唯讀資料來源', true),
    ('en-US', 'sys', 'dict-item', 'ds_use', 'readonly', 'Read-only Data Source', true);

-- dict-item ds_type
insert into "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "namespace", "key", "value", "built_in") values
    ('zh-CN', 'sys', 'dict-item', 'ds_type', 'hikariCP', 'hikariCP', true),
    ('zh-TW', 'sys', 'dict-item', 'ds_type', 'hikariCP', 'hikariCP', true),
    ('en-US', 'sys', 'dict-item', 'ds_type', 'hikariCP', 'hikariCP', true);

-- dict-item resource_type
insert into "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "namespace", "key", "value", "built_in") values
    ('zh-CN', 'sys', 'dict-item', 'resource_type', '2', '功能', true),
    ('zh-TW', 'sys', 'dict-item', 'resource_type', '2', '功能', true),
    ('en-US', 'sys', 'dict-item', 'resource_type', '2', 'Function', true),

    ('zh-CN', 'sys', 'dict-item', 'resource_type', '1', '菜单', true),
    ('zh-TW', 'sys', 'dict-item', 'resource_type', '1', '選單', true),
    ('en-US', 'sys', 'dict-item', 'resource_type', '1', 'Menu', true);

-- dict-item cache_strategy
insert into "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "namespace", "key", "value", "built_in") values
    ('zh-CN', 'sys', 'dict-item', 'cache_strategy', 'SINGLE_LOCAL', '本地单节点', true),
    ('zh-TW', 'sys', 'dict-item', 'cache_strategy', 'SINGLE_LOCAL', '本地單節點', true),
    ('en-US', 'sys', 'dict-item', 'cache_strategy', 'SINGLE_LOCAL', 'Single Local', true),

    ('zh-CN', 'sys', 'dict-item', 'cache_strategy', 'REMOTE', '远程', true),
    ('zh-TW', 'sys', 'dict-item', 'cache_strategy', 'REMOTE', '遠端', true),
    ('en-US', 'sys', 'dict-item', 'cache_strategy', 'REMOTE', 'Remote', true),

    ('zh-CN', 'sys', 'dict-item', 'cache_strategy', 'LOCAL_REMOTE', '本地+远程', true),
    ('zh-TW', 'sys', 'dict-item', 'cache_strategy', 'LOCAL_REMOTE', '本地+遠端', true),
    ('en-US', 'sys', 'dict-item', 'cache_strategy', 'LOCAL_REMOTE', 'Local+Remote', true);

-- dict-item i18n_type
insert into "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "namespace", "key", "value", "built_in") values
    ('zh-CN', 'sys', 'dict-item', 'i18n_type', 'dict', '字典', true),
    ('zh-TW', 'sys', 'dict-item', 'i18n_type', 'dict', '字典', true),
    ('en-US', 'sys', 'dict-item', 'i18n_type', 'dict', 'Dictionary', true),

    ('zh-CN', 'sys', 'dict-item', 'i18n_type', 'dict-item', '字典项', true),
    ('zh-TW', 'sys', 'dict-item', 'i18n_type', 'dict-item', '字典項', true),
    ('en-US', 'sys', 'dict-item', 'i18n_type', 'dict-item', 'Dictionary Item', true),

    ('zh-CN', 'sys', 'dict-item', 'i18n_type', 'valid-msg', '校验失败信息', true),
    ('zh-TW', 'sys', 'dict-item', 'i18n_type', 'valid-msg', '驗證失敗訊息', true),
    ('en-US', 'sys', 'dict-item', 'i18n_type', 'valid-msg', 'Validation failed message', true),

    ('zh-CN', 'sys', 'dict-item', 'i18n_type', 'error-msg', '错误信息', true),
    ('zh-TW', 'sys', 'dict-item', 'i18n_type', 'error-msg', '錯誤訊息', true),
    ('en-US', 'sys', 'dict-item', 'i18n_type', 'error-msg', 'Error message', true),

    ('zh-CN', 'sys', 'dict-item', 'i18n_type', 'view', '页面', true),
    ('zh-TW', 'sys', 'dict-item', 'i18n_type', 'view', '頁面', true),
    ('en-US', 'sys', 'dict-item', 'i18n_type', 'view', 'Page', true);


-- dict-item terminal_type
insert into "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "namespace", "key", "value", "built_in") values
    ('zh-CN', 'sys', 'dict-item', 'terminal_type', '1', 'PC端', true),
    ('zh-TW', 'sys', 'dict-item', 'terminal_type', '1', 'PC端', true),
    ('en-US', 'sys', 'dict-item', 'terminal_type', '1', 'PC', true),

    ('zh-CN', 'sys', 'dict-item', 'terminal_type', '2', '手机端', true),
    ('zh-TW', 'sys', 'dict-item', 'terminal_type', '2', '手機端', true),
    ('en-US', 'sys', 'dict-item', 'terminal_type', '2', 'Mobile', true),

    ('zh-CN', 'sys', 'dict-item', 'terminal_type', '4', '手机端H5-Android', true),
    ('zh-TW', 'sys', 'dict-item', 'terminal_type', '4', '手機端H5-Android', true),
    ('en-US', 'sys', 'dict-item', 'terminal_type', '4', 'Mobile H5-Android', true),

    ('zh-CN', 'sys', 'dict-item', 'terminal_type', '8', '手机端H5-iOS', true),
    ('zh-TW', 'sys', 'dict-item', 'terminal_type', '8', '手機端H5-iOS', true),
    ('en-US', 'sys', 'dict-item', 'terminal_type', '8', 'Mobile H5-iOS', true),

    ('zh-CN', 'sys', 'dict-item', 'terminal_type', '9', '安卓收藏桌面', true),
    ('zh-TW', 'sys', 'dict-item', 'terminal_type', '9', 'Android桌面捷徑', true),
    ('en-US', 'sys', 'dict-item', 'terminal_type', '9', 'Android Home Shortcut', true),

    ('zh-CN', 'sys', 'dict-item', 'terminal_type', '10', 'iOS收藏桌面', true),
    ('zh-TW', 'sys', 'dict-item', 'terminal_type', '10', 'iOS桌面捷徑', true),
    ('en-US', 'sys', 'dict-item', 'terminal_type', '10', 'iOS Home Shortcut', true),

    ('zh-CN', 'sys', 'dict-item', 'terminal_type', '12', '手机端Android', true),
    ('zh-TW', 'sys', 'dict-item', 'terminal_type', '12', '手機端Android', true),
    ('en-US', 'sys', 'dict-item', 'terminal_type', '12', 'Mobile Android', true),

    ('zh-CN', 'sys', 'dict-item', 'terminal_type', '16', '手机端iOS', true),
    ('zh-TW', 'sys', 'dict-item', 'terminal_type', '16', '手機端iOS', true),
    ('en-US', 'sys', 'dict-item', 'terminal_type', '16', 'Mobile iOS', true);

-- dict-item access_rule_type
insert into "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "namespace", "key", "value", "built_in") values
    ('zh-CN', 'sys', 'dict-item', 'access_rule_type', '0', '不限制', true),
    ('zh-TW', 'sys', 'dict-item', 'access_rule_type', '0', '不限制', true),
    ('en-US', 'sys', 'dict-item', 'access_rule_type', '0', 'No Restriction', true),

    ('zh-CN', 'sys', 'dict-item', 'access_rule_type', '1', '白名单', true),
    ('zh-TW', 'sys', 'dict-item', 'access_rule_type', '1', '白名單', true),
    ('en-US', 'sys', 'dict-item', 'access_rule_type', '1', 'Allowlist', true),

    ('zh-CN', 'sys', 'dict-item', 'access_rule_type', '2', '黑名单', true),
    ('zh-TW', 'sys', 'dict-item', 'access_rule_type', '2', '黑名單', true),
    ('en-US', 'sys', 'dict-item', 'access_rule_type', '2', 'Blocklist', true),

    ('zh-CN', 'sys', 'dict-item', 'access_rule_type', '3', '白名单+黑名单', true),
    ('zh-TW', 'sys', 'dict-item', 'access_rule_type', '3', '白名單+黑名單', true),
    ('en-US', 'sys', 'dict-item', 'access_rule_type', '3', 'Allowlist + Blocklist', true);

-- dict-item locale
insert into "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "namespace", "key", "value", "built_in") values
    ('zh-CN', 'sys', 'dict-item', 'dict-item', 'zh-CN', '简体中文', true),
    ('zh-TW', 'sys', 'dict-item', 'dict-item', 'zh-CN', '簡體中文', true),
    ('en-US', 'sys', 'dict-item', 'dict-item', 'zh-CN', 'Simplified Chinese', true),

    ('zh-CN', 'sys', 'dict-item', 'dict-item', 'zh-TW', '繁体中文', true),
    ('zh-TW', 'sys', 'dict-item', 'dict-item', 'zh-TW', '繁體中文', true),
    ('en-US', 'sys', 'dict-item', 'dict-item', 'zh-TW', 'Traditional Chinese', true),

    ('zh-CN', 'sys', 'dict-item', 'dict-item', 'en-US', '英语（美国）', true),
    ('zh-TW', 'sys', 'dict-item', 'dict-item', 'en-US', '英語（美國）', true),
    ('en-US', 'sys', 'dict-item', 'dict-item', 'en-US', 'English (United States)', true),

    ('zh-CN', 'sys', 'dict-item', 'dict-item', 'ja_JP', '日语', true),
    ('zh-TW', 'sys', 'dict-item', 'dict-item', 'ja_JP', '日語', true),
    ('en-US', 'sys', 'dict-item', 'dict-item', 'ja_JP', 'Japanese', true),

    ('zh-CN', 'sys', 'dict-item', 'dict-item', 'ko_KR', '韩语', true),
    ('zh-TW', 'sys', 'dict-item', 'dict-item', 'ko_KR', '韓語', true),
    ('en-US', 'sys', 'dict-item', 'dict-item', 'ko_KR', 'Korean', true),

    ('zh-CN', 'sys', 'dict-item', 'dict-item', 'ru_RU', '俄语', true),
    ('zh-TW', 'sys', 'dict-item', 'dict-item', 'ru_RU', '俄語', true),
    ('en-US', 'sys', 'dict-item', 'dict-item', 'ru_RU', 'Russian', true),

    ('zh-CN', 'sys', 'dict-item', 'dict-item', 'in_ID', '印尼语', true),
    ('zh-TW', 'sys', 'dict-item', 'dict-item', 'in_ID', '印尼語', true),
    ('en-US', 'sys', 'dict-item', 'dict-item', 'in_ID', 'Indonesian', true),

    ('zh-CN', 'sys', 'dict-item', 'dict-item', 'ar_AE', '阿拉伯语（阿联酋）', true),
    ('zh-TW', 'sys', 'dict-item', 'dict-item', 'ar_AE', '阿拉伯語（阿聯酋）', true),
    ('en-US', 'sys', 'dict-item', 'dict-item', 'ar_AE', 'Arabic (United Arab Emirates)', true),

    ('zh-CN', 'sys', 'dict-item', 'dict-item', 'fr_FR', '法语', true),
    ('zh-TW', 'sys', 'dict-item', 'dict-item', 'fr_FR', '法語', true),
    ('en-US', 'sys', 'dict-item', 'dict-item', 'fr_FR', 'French', true),

    ('zh-CN', 'sys', 'dict-item', 'dict-item', 'es_ES', '西班牙语', true),
    ('zh-TW', 'sys', 'dict-item', 'dict-item', 'es_ES', '西班牙語', true),
    ('en-US', 'sys', 'dict-item', 'dict-item', 'es_ES', 'Spanish', true),

    ('zh-CN', 'sys', 'dict-item', 'dict-item', 'pt_BR', '葡萄牙语（巴西）', true),
    ('zh-TW', 'sys', 'dict-item', 'dict-item', 'pt_BR', '葡萄牙語（巴西）', true),
    ('en-US', 'sys', 'dict-item', 'dict-item', 'pt_BR', 'Portuguese (Brazil)', true);


-- view.menu
insert into "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "namespace", "key", "value", "built_in") values
    ('zh-CN', 'sys', 'view', 'menu', 'home', '首页', true),
    ('zh-TW', 'sys', 'view', 'menu', 'home', '首頁', true),
    ('en-US', 'sys', 'view', 'menu', 'home', 'Home', true),

    ('zh-CN', 'sys', 'view', 'menu', 'sys', '系统配置', true),
    ('zh-TW', 'sys', 'view', 'menu', 'sys', '系統設定', true),
    ('en-US', 'sys', 'view', 'menu', 'sys', 'System Settings', true),

    ('zh-CN', 'sys', 'view', 'menu', 'system', '系统', true),
    ('zh-TW', 'sys', 'view', 'menu', 'system', '系統', true),
    ('en-US', 'sys', 'view', 'menu', 'system', 'System', true),

    ('zh-CN', 'sys', 'view', 'menu', 'microService', '微服务', true),
    ('zh-TW', 'sys', 'view', 'menu', 'microService', '微服務', true),
    ('en-US', 'sys', 'view', 'menu', 'microService', 'Microservice', true),

    ('zh-CN', 'sys', 'view', 'menu', 'tenant', '租户', true),
    ('zh-TW', 'sys', 'view', 'menu', 'tenant', '租戶', true),
    ('en-US', 'sys', 'view', 'menu', 'tenant', 'Tenant', true),

    ('zh-CN', 'sys', 'view', 'menu', 'dataSource', '数据源', true),
    ('zh-TW', 'sys', 'view', 'menu', 'dataSource', '資料來源', true),
    ('en-US', 'sys', 'view', 'menu', 'dataSource', 'Data Source', true),

    ('zh-CN', 'sys', 'view', 'menu', 'domain', '域名', true),
    ('zh-TW', 'sys', 'view', 'menu', 'domain', '網域', true),
    ('en-US', 'sys', 'view', 'menu', 'domain', 'Domain Name', true),

    ('zh-CN', 'sys', 'view', 'menu', 'resource', '资源', true),
    ('zh-TW', 'sys', 'view', 'menu', 'resource', '資源', true),
    ('en-US', 'sys', 'view', 'menu', 'resource', 'Resource', true),

    ('zh-CN', 'sys', 'view', 'menu', 'dict', '字典', true),
    ('zh-TW', 'sys', 'view', 'menu', 'dict', '字典', true),
    ('en-US', 'sys', 'view', 'menu', 'dict', 'Dictionary', true),

    ('zh-CN', 'sys', 'view', 'menu', 'cache', '缓存', true),
    ('zh-TW', 'sys', 'view', 'menu', 'cache', '快取', true),
    ('en-US', 'sys', 'view', 'menu', 'cache', 'Cache', true),

    ('zh-CN', 'sys', 'view', 'menu', 'param', '参数', true),
    ('zh-TW', 'sys', 'view', 'menu', 'param', '參數', true),
    ('en-US', 'sys', 'view', 'menu', 'param', 'Parameter', true),

    ('zh-CN', 'sys', 'view', 'menu', 'i18n', '国际化', true),
    ('zh-TW', 'sys', 'view', 'menu', 'i18n', '國際化', true),
    ('en-US', 'sys', 'view', 'menu', 'i18n', 'I18N', true),

    ('zh-CN', 'sys', 'view', 'menu', 'accessRule', '访问规则', true),
    ('zh-TW', 'sys', 'view', 'menu', 'accessRule', '存取規則', true),
    ('en-US', 'sys', 'view', 'menu', 'accessRule', 'Access Rule', true);

-- validation message
insert into "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "namespace", "key", "value", "built_in") values
    ('zh-CN', 'sys', 'valid-msg', 'default', 'NotBlank', '不能为空', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'NotBlank', '不得空白', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'NotBlank', 'must not be blank', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'AssertFalse', '只能为false', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'AssertFalse', '必須是 false', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'AssertFalse', 'must be false', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'AssertTrue', '只能为true', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'AssertTrue', '必須是 true', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'AssertTrue', 'must be true', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'DecimalMax', '必须小于${inclusive == true ? ''或等于'' : ''''}{value}', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'DecimalMax', '必須小於 ${inclusive == true ? ''or equal to '' : ''''}{value}', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'DecimalMax', 'must be less than ${inclusive == true ? ''or equal to '' : ''''}{value}', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'DecimalMin', '必须大于${inclusive == true ? ''或等于'' : ''''}{value}', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'DecimalMin', '必須大於 ${inclusive == true ? ''or equal to '' : ''''}{value}', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'DecimalMin', 'must be greater than ${inclusive == true ? ''or equal to '' : ''''}{value}', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Digits', '数字的值超出了允许范围(只允许在{integer}位整数和{fraction}位小数范围内)', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Digits', '數值超出範圍（應為最多 {integer} 位整數與 {fraction} 位小數）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Digits', 'numeric value out of bounds (<{integer} digits>.<{fraction} digits> expected)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Email', '不是一个合法的电子邮件地址', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Email', '必須是形式完整的電子郵件地址', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Email', 'must be a well-formed email address', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Future', '需要是一个将来的时间', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Future', '必須是未來的日期', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Future', 'must be a future date', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'FutureOrPresent', '需要是一个将来或现在的时间', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'FutureOrPresent', '必須是當天或未來的日期', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'FutureOrPresent', 'must be a date in the present or in the future', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Max', '最大不能超过{value}', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Max', '必須小於或等於 {value}', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Max', 'must be less than or equal to {value}', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Min', '最小不能小于{value}', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Min', '必須大於或等於 {value}', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Min', 'must be greater than or equal to {value}', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Negative', '必须是负数', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Negative', '必須小於 0', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Negative', 'must be less than 0', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'NegativeOrZero', '必须是负数或零', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'NegativeOrZero', '必須小於或等於 0', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'NegativeOrZero', 'must be less than or equal to 0', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'NotEmpty', '不能为空', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'NotEmpty', '不得是空的', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'NotEmpty', 'must not be empty', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'NotNull', '不能为null', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'NotNull', '不得是空值', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'NotNull', 'must not be null', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Null', '必须为null', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Null', '必須是空值', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Null', 'must be null', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Past', '需要是一个过去的时间', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Past', '必須是過去的日期', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Past', 'must be a past date', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'PastOrPresent', '需要是一个过去或现在的时间', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'PastOrPresent', '必須是過去或當天的日期', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'PastOrPresent', 'must be a date in the past or in the present', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern', '需要匹配正则表达式"{regexp}"', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern', '必須符合 "{regexp}"', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern', 'must match "{regexp}"', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Positive', '必须是正数', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Positive', '必須大於 0', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Positive', 'must be greater than 0', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'PositiveOrZero', '必须是正数或零', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'PositiveOrZero', '必須大於或等於 0', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'PositiveOrZero', 'must be greater than or equal to 0', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Size', '个数必须在{min}和{max}之间', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Size', '大小必須在 {min} 和 {max} 之間', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Size', 'size must be between {min} and {max}', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'CreditCardNumber', '不合法的信用卡号码', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'CreditCardNumber', '無效的信用卡卡號', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'CreditCardNumber', 'invalid credit card number', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Currency', '不合法的货币 (必须是{value}其中之一)', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Currency', '無效的貨幣（必須是 {value} 之一）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Currency', 'invalid currency (must be one of {value})', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'EAN', '不合法的{type}条形码', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'EAN', '無效的 {type} 條碼', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'EAN', 'invalid {type} barcode', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Length', '长度需要在{min}和{max}之间', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Length', '長度必須在 {min} 和 {max} 之間', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Length', 'length must be between {min} and {max}', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'CodePointLength', '长度需要在{min}和{max}之间', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'CodePointLength', '長度必須在 {min} 和 {max} 之間', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'CodePointLength', 'length must be between {min} and {max}', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'LuhnCheck', '${validatedValue}的校验码不合法, Luhn模10校验和不匹配', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'LuhnCheck', '${validatedValue} 的檢查碼無效，Luhn 模數 10 總和檢查失敗', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'LuhnCheck', 'the check digit for ${validatedValue} is invalid, Luhn Modulo 10 checksum failed', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Mod10Check', '${validatedValue}的校验码不合法, 模10校验和不匹配', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Mod10Check', '${validatedValue} 的檢查碼無效，模數 10 總和檢查失敗', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Mod10Check', 'the check digit for ${validatedValue} is invalid, Modulo 10 checksum failed', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Mod11Check', '${validatedValue}的校验码不合法, 模11校验和不匹配', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Mod11Check', '${validatedValue} 的檢查碼無效，模數 11 總和檢查失敗', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Mod11Check', 'the check digit for ${validatedValue} is invalid, Modulo 11 checksum failed', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'ParametersScriptAssert', '执行脚本表达式"{script}"没有返回期望结果', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'ParametersScriptAssert', 'Script 表示式 "{script}" 不是求值為 true', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'ParametersScriptAssert', 'script expression "{script}" didn''t evaluate to true', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Range', '需要在{min}和{max}之间', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Range', '必須在 {min} 和 {max} 之間', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Range', 'must be between {min} and {max}', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'ScriptAssert', '执行脚本表达式"{script}"没有返回期望结果', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'ScriptAssert', 'Script 表示式 "{script}" 不是求值為 true', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'ScriptAssert', 'script expression "{script}" didn''t evaluate to true', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'URL', '需要是一个合法的URL', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'URL', '必須是有效的 URL', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'URL', 'must be a valid URL', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'time.DurationMax', '必须小于${inclusive == true ? ''或等于'' : ''''}${days == 0 ? '''' : days += ''天''}${hours == 0 ? '''' : hours += ''小时''}${minutes == 0 ? '''' : minutes += ''分钟''}${seconds == 0 ? '''' : seconds += ''秒''}${millis == 0 ? '''' : millis += ''毫秒''}${nanos == 0 ? '''' : nanos += ''纳秒''}${days == 0 && hours == 0 && minutes == 0 && seconds == 0 && millis == 0 && nanos == 0 ? '' 0'' : ''''}', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'time.DurationMax', '必須短於 ${inclusive == true ? '' or equal to'' : ''''}${days == 0 ? '''' : days == 1 ? '' 1 day'' : '' '' += days += '' days''}${hours == 0 ? '''' : hours == 1 ? '' 1 hour'' : '' '' += hours += '' hours''}${minutes == 0 ? '''' : minutes == 1 ? '' 1 minute'' : '' '' += minutes += '' minutes''}${seconds == 0 ? '''' : seconds == 1 ? '' 1 second'' : '' '' += seconds += '' seconds''}${millis == 0 ? '''' : millis == 1 ? '' 1 milli'' : '' '' += millis += '' millis''}${nanos == 0 ? '''' : nanos == 1 ? '' 1 nano'' : '' '' += nanos += '' nanos''}${days == 0 && hours == 0 && minutes == 0 && seconds == 0 && millis == 0 && nanos == 0 ? '' 0'' : ''''}', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'time.DurationMax', 'must be shorter than${inclusive == true ? '' or equal to'' : ''''}${days == 0 ? '''' : days == 1 ? '' 1 day'' : '' '' += days += '' days''}${hours == 0 ? '''' : hours == 1 ? '' 1 hour'' : '' '' += hours += '' hours''}${minutes == 0 ? '''' : minutes == 1 ? '' 1 minute'' : '' '' += minutes += '' minutes''}${seconds == 0 ? '''' : seconds == 1 ? '' 1 second'' : '' '' += seconds += '' seconds''}${millis == 0 ? '''' : millis == 1 ? '' 1 milli'' : '' '' += millis += '' millis''}${nanos == 0 ? '''' : nanos == 1 ? '' 1 nano'' : '' '' += nanos += '' nanos''}${days == 0 && hours == 0 && minutes == 0 && seconds == 0 && millis == 0 && nanos == 0 ? '' 0'' : ''''}', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'time.DurationMin', '必须大于${inclusive == true ? ''或等于'' : ''''}${days == 0 ? '''' : days += ''天''}${hours == 0 ? '''' : hours += ''小时''}${minutes == 0 ? '''' : minutes += ''分钟''}${seconds == 0 ? '''' : seconds += ''秒''}${millis == 0 ? '''' : millis += ''毫秒''}${nanos == 0 ? '''' : nanos += ''纳秒''}${days == 0 && hours == 0 && minutes == 0 && seconds == 0 && millis == 0 && nanos == 0 ? '' 0'' : ''''}', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'time.DurationMin', '必須長於 ${inclusive == true ? '' or equal to'' : ''''}${days == 0 ? '''' : days == 1 ? '' 1 day'' : '' '' += days += '' days''}${hours == 0 ? '''' : hours == 1 ? '' 1 hour'' : '' '' += hours += '' hours''}${minutes == 0 ? '''' : minutes == 1 ? '' 1 minute'' : '' '' += minutes += '' minutes''}${seconds == 0 ? '''' : seconds == 1 ? '' 1 second'' : '' '' += seconds += '' seconds''}${millis == 0 ? '''' : millis == 1 ? '' 1 milli'' : '' '' += millis += '' millis''}${nanos == 0 ? '''' : nanos == 1 ? '' 1 nano'' : '' '' += nanos += '' nanos''}${days == 0 && hours == 0 && minutes == 0 && seconds == 0 && millis == 0 && nanos == 0 ? '' 0'' : ''''}', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'time.DurationMin', 'must be longer than${inclusive == true ? '' or equal to'' : ''''}${days == 0 ? '''' : days == 1 ? '' 1 day'' : '' '' += days += '' days''}${hours == 0 ? '''' : hours == 1 ? '' 1 hour'' : '' '' += hours += '' hours''}${minutes == 0 ? '''' : minutes == 1 ? '' 1 minute'' : '' '' += minutes += '' minutes''}${seconds == 0 ? '''' : seconds == 1 ? '' 1 second'' : '' '' += seconds += '' seconds''}${millis == 0 ? '''' : millis == 1 ? '' 1 milli'' : '' '' += millis += '' millis''}${nanos == 0 ? '''' : nanos == 1 ? '' 1 nano'' : '' '' += nanos += '' nanos''}${days == 0 && hours == 0 && minutes == 0 && seconds == 0 && millis == 0 && nanos == 0 ? '' 0'' : ''''}', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'ISBN', '无效的ISBN', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'ISBN', '無效的 ISBN', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'ISBN', 'invalid ISBN', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'UniqueElements', '只能包含唯一元素', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'UniqueElements', '只能包含唯一元素', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'UniqueElements', 'must only contain unique elements', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'CNPJ', '无效的巴西公司纳税登记码 (CNPJ)', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'CNPJ', '無效的巴西公司納稅登記碼 (CNPJ)', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'CNPJ', 'invalid Brazilian corporate taxpayer registry number (CNPJ)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'CPF', '效的巴西个人纳稅登记码 (CPF)', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'CPF', '無效的巴西個人納稅登記碼 (CPF)', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'CPF', 'invalid Brazilian individual taxpayer registry number (CPF)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'TituloEleitoral', '无效的巴西选民 ID 卡号', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'TituloEleitoral', '無效的巴西選民 ID 卡號', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'TituloEleitoral', 'invalid Brazilian Voter ID card number', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'REGON', '无效的波兰纳稅人识別码 (REGON)', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'REGON', '無效的波蘭納稅人識別碼 (REGON)', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'REGON', 'invalid Polish Taxpayer Identification Number (REGON)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'NIP', '无效的 VAT 识別码 (NIP)', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'NIP', '無效的 VAT 識別碼 (NIP)', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'NIP', 'invalid VAT Identification Number (NIP)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'PESEL', '无效的波兰国家识別码 (PESEL)', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'PESEL', '無效的波蘭國家識別碼 (PESEL)', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'PESEL', 'invalid Polish National Identification Number (PESEL)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'IpAddress', '无效的IP地址', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'IpAddress', '無效的 IP 位址', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'IpAddress', 'invalid IP address', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Normalized', '必须为规范化格式', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Normalized', '必須為正規化格式', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Normalized', 'must be normalized', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'UUID', '必须为有效的UUID', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'UUID', '必須為有效的 UUID', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'UUID', 'must be a valid UUID', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'KorRRN', '无效的韩国居民登记号码(KorRRN)', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'KorRRN', '無效的韓國居民登記號碼 (KorRRN)', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'KorRRN', 'invalid Korean resident registration number (KorRRN)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'INN', '无效的俄罗斯纳税人识别号(INN)', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'INN', '無效的俄羅斯納稅人識別號 (INN)', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'INN', 'invalid Russian taxpayer identification number (INN)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'BitcoinAddress', '必须是有效的比特币地址', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'BitcoinAddress', '必須是有效的比特幣位址', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'BitcoinAddress', 'must be a valid Bitcoin address', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'AtLeast', '至少需要{count}个', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'AtLeast', '至少需要 {count} 個', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'AtLeast', 'at least {count} items required', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'CnIdCardNo', '无效的中国大陆身份证号码', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'CnIdCardNo', '無效的中國大陸身分證號碼', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'CnIdCardNo', 'invalid Mainland China ID number', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'DateTime', '日期的格式应为{format}', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'DateTime', '日期格式應為 {format}', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'DateTime', 'the date format should be {format}', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'DictEnumItemCode', '不是枚举{enumClass}中定义的字典项', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'DictEnumItemCode', '不是列舉 {enumClass} 中定義的字典項目', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'DictEnumItemCode', 'not a dictionary item defined in enum {enumClass}', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'DictItemCode', '不是为{atomicServiceCode}中{dictType}定义的字典项', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'DictItemCode', '不是為 {atomicServiceCode} 中 {dictType} 定義的字典項目', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'DictItemCode', 'not a dictionary item defined for {dictType} in {atomicServiceCode}', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'MaxSize', '个数不得大于{max}', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'MaxSize', '數量不得大於 {max}', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'MaxSize', 'the count must not exceed {max}', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'MaxLength', '长度不得大于{max}', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'MaxLength', '長度不得大於 {max}', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'MaxLength', 'length must not exceed {max}', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'FixedLength', '长度必须为{value}个字符', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'FixedLength', '長度必須為 {value} 個字元', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'FixedLength', 'length must be exactly {value} characters', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::cn-mainland-mobile', '须为中国大陆11位手机号码', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::cn-mainland-mobile', '須為中國大陸 11 位手機號碼', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::cn-mainland-mobile', 'must be an 11-digit mainland China mobile number', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::qq-number', '须为5～11位数字的QQ号', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::qq-number', '須為 5～11 位數字的 QQ 號碼', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::qq-number', 'must be a QQ number with 5–11 digits', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::phone-digits-7-20', '须为7～20位数字的手机或电话号码', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::phone-digits-7-20', '須為 7～20 位數字的手機或電話號碼', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::phone-digits-7-20', 'must be 7–20 digits (mobile or phone)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::ipv4', '须为合法的IPv4地址', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::ipv4', '須為合法的 IPv4 位址', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::ipv4', 'must be a valid IPv4 address', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::http-url', '须为合法的URL地址', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::http-url', '須為合法的 URL', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::http-url', 'must be a valid URL', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::tel-or-cn-mobile', '须为合法的电话或手机号码', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::tel-or-cn-mobile', '須為合法的電話或手機號碼', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::tel-or-cn-mobile', 'must be a valid landline or mobile number', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::short-person-name', '须为2～30位姓名（汉字、英文字母与间隔号·）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::short-person-name', '須為 2～30 字姓名（漢字、英文字母與間隔號·）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::short-person-name', 'must be 2–30 characters (Chinese, Latin letters, or middle dot)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::real-person-name', '须符合真实姓名格式（中英日文、空格与分隔符，且不能为纯数字）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::real-person-name', '須符合真實姓名格式（中英日文、空格與分隔符，且不得為純數字）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::real-person-name', 'must match real-name rules (multi-script; not all digits)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::bank-account-holder-name', '须符合银行账户姓名格式', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::bank-account-holder-name', '須符合銀行帳戶姓名格式', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::bank-account-holder-name', 'must match bank account holder name format', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::payer-display-name', '须符合存款人姓名格式', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::payer-display-name', '須符合存款人姓名格式', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::payer-display-name', 'must match payer display name format', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::text-without-special-chars', '不能包含 & * = | { } < > / … — 等特殊字符', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::text-without-special-chars', '不得包含 & * = | { } < > / … — 等特殊字元', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::text-without-special-chars', 'must not contain special characters such as & * = | { } < > / … —', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::email', '须为合法的电子邮箱地址', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::email', '須為合法的電子郵件地址', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::email', 'must be a valid email address', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::mail-or-cn-mobile', '须为邮箱或中国大陆手机号', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::mail-or-cn-mobile', '須為電子郵件或中國大陸手機號碼', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::mail-or-cn-mobile', 'must be an email or mainland China mobile number', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::cn-landline-phone', '须为合法的固定电话号码（含区号与分机格式）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::cn-landline-phone', '須為合法的市話號碼（含區碼與分機格式）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::cn-landline-phone', 'must be a valid landline number (area code / extension format)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::login-password', '须为6～20位登录密码（字母、数字及允许符号）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::login-password', '須為 6～20 位登入密碼（字母、數字及允許符號）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::login-password', 'must be 6–20 characters (letters, digits, allowed symbols)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::security-pin-six-digits', '须为6位数字安全密码', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::security-pin-six-digits', '須為 6 位數字安全密碼', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::security-pin-six-digits', 'must be a 6-digit security code', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::positive-int-text', '须为正整数', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::positive-int-text', '須為正整數', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::positive-int-text', 'must be a positive integer', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::ipv4-semicolon-list', '须为分号分隔的多个IPv4地址', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::ipv4-semicolon-list', '須為分號分隔的多個 IPv4 位址', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::ipv4-semicolon-list', 'must be IPv4 addresses separated by semicolons', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::ipv6-full', '须为标准全写IPv6地址', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::ipv6-full', '須為標準全寫 IPv6 位址', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::ipv6-full', 'must be a full standard IPv6 address', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::ipv6-compact', '须为合法的压缩写法IPv6地址（含::）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::ipv6-compact', '須為合法的壓縮寫法 IPv6 位址（含::）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::ipv6-compact', 'must be a valid compressed IPv6 address (with ::)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::single-char-repeated', '须为同一字符连续重复（如aaa）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::single-char-repeated', '須為同一字元連續重複', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::single-char-repeated', 'must be repeated occurrences of a single character', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::digits-non-empty', '须为纯数字（至少一位）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::digits-non-empty', '須為純數字（至少一位）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::digits-non-empty', 'must contain digits only (at least one)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::latin-letters-only', '须为纯英文字母', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::latin-letters-only', '須為純英文字母', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::latin-letters-only', 'must be Latin letters only', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::latin-lowercase-only', '须为纯小写英文字母', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::latin-lowercase-only', '須為純小寫英文字母', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::latin-lowercase-only', 'must be lowercase Latin letters only', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::lowercase-alnum-not-all-digits', '须为小写字母与数字组合且不能为纯数字', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::lowercase-alnum-not-all-digits', '須為小寫字母與數字組合且不得為純數字', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::lowercase-alnum-not-all-digits', 'must be lowercase letters and digits, not all digits', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::nick-name', '须为3～15位昵称（中文、英文与数字）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::nick-name', '須為 3～15 字暱稱（中文、英文與數字）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::nick-name', 'must be 3–15 characters (Chinese, Latin, digits)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::msn', '须为合法的MSN账号（规则同邮箱）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::msn', '須為合法的 MSN 帳號（規則同電子郵件）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::msn', 'must be a valid MSN identifier (same rules as email)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::password-strength-letters-only', '密码强度不足：仅包含字母', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::password-strength-letters-only', '密碼強度不足：僅包含字母', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::password-strength-letters-only', 'password too weak: letters only', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::password-strength-digits-only', '密码强度不足：仅包含数字', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::password-strength-digits-only', '密碼強度不足：僅包含數字', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::password-strength-digits-only', 'password too weak: digits only', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::password-strength-letters-and-digits', '密码须同时包含字母与数字', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::password-strength-letters-and-digits', '密碼須同時包含字母與數字', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::password-strength-letters-and-digits', 'must contain both letters and digits', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::password-strength-with-symbols', '须包含字母、数字及允许的符号', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::password-strength-with-symbols', '須包含字母、數字及允許的符號', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::password-strength-with-symbols', 'must include letters, digits, and allowed symbols', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::score-or-handicap-text', '须为合法的让分或比分格式', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::score-or-handicap-text', '須為合法的讓分或比分格式', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::score-or-handicap-text', 'must be a valid handicap or score format', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::positive-decimal-text', '须为正数（整数或小数，格式合法）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::positive-decimal-text', '須為正數（整數或小數，格式合法）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::positive-decimal-text', 'must be a positive number in valid format', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::positive-number-text', '须为合法的整数或小数（正数）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::positive-number-text', '須為合法的整數或小數（正數）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::positive-number-text', 'must be a valid positive integer or decimal', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::signed-integer-text', '须为整数（可带正负号）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::signed-integer-text', '須為整數（可含正負號）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::signed-integer-text', 'must be an integer (optional sign)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::bank-card-number', '须为10～25位数字的银行卡号', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::bank-card-number', '須為 10～25 位數字的銀行卡號', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::bank-card-number', 'must be a bank card number with 10–25 digits', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::btc-amount-text', '须为合法的比特币数量（精度与下限符合规则）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::btc-amount-text', '須為合法的比特幣數量（精度與下限符合規則）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::btc-amount-text', 'must be a valid Bitcoin amount per precision rules', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::site-ids-comma-separated', '须为逗号分隔的数字站点ID、单个数字，或为空', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::site-ids-comma-separated', '須為逗號分隔的數字站台 ID、單一數字，或為空', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::site-ids-comma-separated', 'must be comma-separated numeric site IDs, a single number, or empty', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::empty-or-positive-int-text', '须为空或正整数', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::empty-or-positive-int-text', '須為空或正整數', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::empty-or-positive-int-text', 'must be empty or a positive integer', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::digits-at-most-9', '须为至多9位数字', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::digits-at-most-9', '須為至多 9 位數字', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::digits-at-most-9', 'must be at most 9 digits', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::signed-amount-loose', '须为合法金额（可选负号，最多两位小数）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::signed-amount-loose', '須為合法金額（可選負號，最多兩位小數）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::signed-amount-loose', 'must be a valid amount (optional minus; up to 2 decimal places)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::wechat-id', '须为合法的微信号', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::wechat-id', '須為合法的微信帳號', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::wechat-id', 'must be a valid WeChat ID', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::game-player-account', '须为合法的玩家账号（含游客）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::game-player-account', '須為合法的玩家帳號（含訪客）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::game-player-account', 'must be a valid player account (including guest)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::uuid-hyphenated', '须为合法UUID（8-4-4-4-12）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::uuid-hyphenated', '須為合法 UUID（8-4-4-4-12）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::uuid-hyphenated', 'must be a valid UUID (8-4-4-4-12)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::domain-list-comma-separated', '须为逗号分隔的域名列表格式', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::domain-list-comma-separated', '須為逗號分隔的網域名稱清單格式', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::domain-list-comma-separated', 'must match comma-separated domain list format', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::amount-nonzero-two-decimals', '须为合法金额（非零，最多两位小数）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::amount-nonzero-two-decimals', '須為合法金額（非零，最多兩位小數）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::amount-nonzero-two-decimals', 'must be a non-zero amount with up to 2 decimal places', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::text-starts-with-digit', '须以数字开头', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::text-starts-with-digit', '須以數字開頭', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::text-starts-with-digit', 'must start with a digit', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::digits-only-optional-empty', '仅允许数字（可为空）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::digits-only-optional-empty', '僅允許數字（可為空）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::digits-only-optional-empty', 'digits only (may be empty)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::han-latin-alnum', '仅允许中文、英文字母与数字', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::han-latin-alnum', '僅允許中文、英文字母與數字', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::han-latin-alnum', 'Chinese characters, Latin letters, and digits only', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::date-iso-yyyy-mm-dd', '须为 yyyy-MM-dd 格式的日期', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::date-iso-yyyy-mm-dd', '須為 yyyy-MM-dd 格式的日期', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::date-iso-yyyy-mm-dd', 'must be a date in yyyy-MM-dd format', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::time-24h-mm-optional-ss', '须为 24 小时制时间（HH:mm 或 HH:mm:ss）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::time-24h-mm-optional-ss', '須為 24 小時制時間（HH:mm 或 HH:mm:ss）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::time-24h-mm-optional-ss', 'must be 24-hour time (HH:mm or HH:mm:ss)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::cn-mainland-postal-code', '须为 6 位数字的中国大陆邮政编码', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::cn-mainland-postal-code', '須為 6 位數字的中國大陸郵遞區號', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::cn-mainland-postal-code', 'must be a 6-digit mainland China postal code', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::hex-color-css', '须为 #RGB、#RRGGBB 或 #RRGGBBAA 形式的十六进制颜色', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::hex-color-css', '須為 #RGB、#RRGGBB 或 #RRGGBBAA 形式的十六進位色彩', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::hex-color-css', 'must be a hex color (#RGB, #RRGGBB, or #RRGGBBAA)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::mac-address-colon-or-hyphen', '须为合法的 MAC 地址（冒号或连字符分组）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::mac-address-colon-or-hyphen', '須為合法的 MAC 位址（冒號或連字號分組）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::mac-address-colon-or-hyphen', 'must be a valid MAC address (colon- or hyphen-separated pairs)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::network-port-1-65535', '须为 1～65535 的端口号', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::network-port-1-65535', '須為 1～65535 的連接埠編號', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::network-port-1-65535', 'must be a port number from 1 through 65535', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::slug-kebab-lowercase', '须为小写字母、数字与连字符组成的 slug（不以连字符开头或结尾）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::slug-kebab-lowercase', '須為小寫字母、數字與連字號組成的 slug（不以連字號開頭或結尾）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::slug-kebab-lowercase', 'must be a lowercase kebab-case slug (letters, digits, hyphens; no leading/trailing hyphen)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::latin-alnum-only', '仅允许英文字母与数字（无空格或符号）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::latin-alnum-only', '僅允許英文字母與數字（無空格或符號）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::latin-alnum-only', 'Latin letters and digits only (no spaces or symbols)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::ipv4-cidr-notation', '须为 IPv4 CIDR 表示法（地址/前缀长度 0～32）', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::ipv4-cidr-notation', '須為 IPv4 CIDR 表示法（位址/前置長度 0～32）', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::ipv4-cidr-notation', 'must be IPv4 in CIDR notation (address / prefix 0–32)', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::percent-integer-0-100', '须为 0～100 的整数百分比', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::percent-integer-0-100', '須為 0～100 的整數百分比', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::percent-integer-0-100', 'must be an integer percentage from 0 through 100', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::latin-alnum-dash-underscore', '仅允许英文字母、数字、连字符与下划线', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::latin-alnum-dash-underscore', '僅允許英文字母、數字、連字號與底線', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::latin-alnum-dash-underscore', 'Latin letters, digits, hyphens, and underscores only', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::relaxed-var-name', '仅允许英文字母、数字、连字符与下划线，且必须以字母或下划线开头', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::relaxed-var-name', '僅允許英文字母、數字、連字號與底線，且必須以字母或底線開頭', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::relaxed-var-name', 'Latin letters, digits, hyphens, and underscores only; must start with a letter or underscore', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::var-name', '只允许字母、数字、下划线，且不能以数字开头', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::var-name', '只允許英文字母、數字與底線，且不得以數字開頭', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::var-name', 'only letters, numbers, and underscores; cannot start with a number', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::jdbc-url', '须为合法的JDBC连接地址', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::jdbc-url', '須為合法的 JDBC 連線位址', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::jdbc-url', 'must be a valid JDBC URL', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::domain', '须为合法的域名', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::domain', '須為合法的網域名稱', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::domain', 'must be a valid domain name', true),

    ('zh-CN', 'sys', 'valid-msg', 'default', 'Pattern::context', '必须以 / 开头，且仅由小写字母、数字、短横线和分级斜杠组成', true),
    ('zh-TW', 'sys', 'valid-msg', 'default', 'Pattern::context', '需以 / 開頭，後續只可使用小寫英文字母、數字及連字號 -，並以 / 區分層級', true),
    ('en-US', 'sys', 'valid-msg', 'default', 'Pattern::context', 'must start with /; only lowercase letters, digits, hyphens, and / separators are allowed', true);

insert into "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "namespace", "key", "value", "built_in") values
    ('zh-CN', 'sys', 'valid-msg', 'dataSource', 'Compare::maxActive', '不得小于初始连接数', true),
    ('zh-TW', 'sys', 'valid-msg', 'dataSource', 'Compare::maxActive', '不得小於初始連線數', true),
    ('en-US', 'sys', 'valid-msg', 'dataSource', 'Compare::maxActive', 'must not be less than initial size', true),

    ('zh-CN', 'sys', 'valid-msg', 'dataSource', 'Compare::maxIdle', '不得大于最大连接数', true),
    ('zh-TW', 'sys', 'valid-msg', 'dataSource', 'Compare::maxIdle', '不得大於最大連線數', true),
    ('en-US', 'sys', 'valid-msg', 'dataSource', 'Compare::maxIdle', 'must not exceed max active', true),

    ('zh-CN', 'sys', 'valid-msg', 'dataSource', 'Compare::minIdle', '不得大于最大连接数', true),
    ('zh-TW', 'sys', 'valid-msg', 'dataSource', 'Compare::minIdle', '不得大於最大連線數', true),
    ('en-US', 'sys', 'valid-msg', 'dataSource', 'Compare::minIdle', 'must not exceed max active', true);


-- error-msg
insert into "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "namespace", "key", "value", "built_in") values
    ('zh-CN', 'sys', 'error-msg', 'default', '200', '操作成功', true),
    ('zh-TW', 'sys', 'error-msg', 'default', '200', '操作成功', true),
    ('en-US', 'sys', 'error-msg', 'default', '200', 'Operation completed successfully', true),

    ('zh-CN', 'sys', 'error-msg', 'default', '400', '请求有误，请检查后重试', true),
    ('zh-TW', 'sys', 'error-msg', 'default', '400', '請求有誤，請檢查後再試', true),
    ('en-US', 'sys', 'error-msg', 'default', '400', 'There is an issue with your request. Please check and try again', true),

    ('zh-CN', 'sys', 'error-msg', 'default', '401', '请先登录', true),
    ('zh-TW', 'sys', 'error-msg', 'default', '401', '請先登入', true),
    ('en-US', 'sys', 'error-msg', 'default', '401', 'Please sign in first', true),

    ('zh-CN', 'sys', 'error-msg', 'default', '403', '抱歉，您暂无权限执行此操作', true),
    ('zh-TW', 'sys', 'error-msg', 'default', '403', '抱歉，您目前沒有權限執行此操作', true),
    ('en-US', 'sys', 'error-msg', 'default', '403', 'Sorry, you do not have permission to perform this action', true),

    ('zh-CN', 'sys', 'error-msg', 'default', '404', '抱歉，您访问的内容不存在', true),
    ('zh-TW', 'sys', 'error-msg', 'default', '404', '抱歉，您要存取的內容不存在', true),
    ('en-US', 'sys', 'error-msg', 'default', '404', 'Sorry, the content you are looking for does not exist', true),

    ('zh-CN', 'sys', 'error-msg', 'default', '405', '当前请求方式不支持，请换一种方式再试', true),
    ('zh-TW', 'sys', 'error-msg', 'default', '405', '目前不支援此請求方式，請改用其他方式再試', true),
    ('en-US', 'sys', 'error-msg', 'default', '405', 'This request method is not supported. Please try a different one', true),

    ('zh-CN', 'sys', 'error-msg', 'default', '4001', '您填写的信息有误，请检查后重试', true),
    ('zh-TW', 'sys', 'error-msg', 'default', '4001', '您填寫的資訊有誤，請檢查後再試', true),
    ('en-US', 'sys', 'error-msg', 'default', '4001', 'Some of the information you entered is invalid. Please check and try again', true),

    ('zh-CN', 'sys', 'error-msg', 'default', '4002', '操作未完成，请稍后再试', true),
    ('zh-TW', 'sys', 'error-msg', 'default', '4002', '操作未完成，請稍後再試', true),
    ('en-US', 'sys', 'error-msg', 'default', '4002', 'The action could not be completed. Please try again later', true),

    ('zh-CN', 'sys', 'error-msg', 'default', '4003', '内置记录不可删除', true),
    ('zh-TW', 'sys', 'error-msg', 'default', '4003', '內建記錄不可刪除', true),
    ('en-US', 'sys', 'error-msg', 'default', '4003', 'Built-in records cannot be deleted', true),

    ('zh-CN', 'sys', 'error-msg', 'default', '500', '系统开小差了，请稍后再试', true),
    ('zh-TW', 'sys', 'error-msg', 'default', '500', '系統出了點狀況，請稍後再試', true),
    ('en-US', 'sys', 'error-msg', 'default', '500', 'Something went wrong on our side. Please try again later', true),

    ('zh-CN', 'sys', 'error-msg', 'cache', 'SC00000001', '缓存键不存在', true),
    ('zh-TW', 'sys', 'error-msg', 'cache', 'SC00000001', '快取鍵不存在', true),
    ('en-US', 'sys', 'error-msg', 'cache', 'SC00000001', 'Cache key does not exist', true),

    ('zh-CN', 'sys', 'error-msg', 'cache', 'SC00000002', '缓存配置不存在', true),
    ('zh-TW', 'sys', 'error-msg', 'cache', 'SC00000002', '快取設定不存在', true),
    ('en-US', 'sys', 'error-msg', 'cache', 'SC00000002', 'Cache configuration does not exist', true);

--endregion DML
