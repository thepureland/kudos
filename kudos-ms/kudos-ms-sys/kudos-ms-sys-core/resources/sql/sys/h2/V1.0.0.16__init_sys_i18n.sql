--region DDL
create table if not exists "sys_i18n"
(
    "id"                  character(36) default RANDOM_UUID() not null primary key,
    "locale"              character varying(5)                not null,
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

    ('zh-CN', 'sys', 'valid-msg', 'cache', 'Pattern::var-name', '只允许字母、数字、下划线，且不能以数字开头', true),
    ('zh-TW', 'sys', 'valid-msg', 'cache', 'Pattern::var-name', '只允許英文字母、數字與底線，且不得以數字開頭', true),
    ('en-US', 'sys', 'valid-msg', 'cache', 'Pattern::var-name', 'only letters, numbers, and underscores; cannot start with a number', true);

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

    ('zh-CN', 'sys', 'error-msg', 'default', '500', '系统开小差了，请稍后再试', true),
    ('zh-TW', 'sys', 'error-msg', 'default', '500', '系統出了點狀況，請稍後再試', true),
    ('en-US', 'sys', 'error-msg', 'default', '500', 'Something went wrong on our side. Please try again later', true);

--endregion DML
