--region DML

-- dict
insert into "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "namespace", "key", "value", "built_in") values
    ('zh-CN', 'kudos-user', 'dict', 'dict', 'account_type', '账号类型', true),
    ('zh-TW', 'kudos-user', 'dict', 'dict', 'account_type', '帳號類型', true),
    ('en-US', 'kudos-user', 'dict', 'dict', 'account_type', 'Account Type', true),

    ('zh-CN', 'kudos-user', 'dict', 'dict', 'account_status', '账号状态', true),
    ('zh-TW', 'kudos-user', 'dict', 'dict', 'account_status', '帳號狀態', true),
    ('en-US', 'kudos-user', 'dict', 'dict', 'account_status', 'Account Status', true),

    ('zh-CN', 'kudos-user', 'dict', 'dict', 'account_provider', '第三方账号提供商', true),
    ('zh-TW', 'kudos-user', 'dict', 'dict', 'account_provider', '第三方帳號提供商', true),
    ('en-US', 'kudos-user', 'dict', 'dict', 'account_provider', 'Third-party Account Provider', true),

    ('zh-CN', 'kudos-user', 'dict', 'dict', 'org_type', '机构类型', true),
    ('zh-TW', 'kudos-user', 'dict', 'dict', 'org_type', '機構類型', true),
    ('en-US', 'kudos-user', 'dict', 'dict', 'org_type', 'Organization Type', true),

    ('zh-CN', 'kudos-user', 'dict', 'dict', 'contact_way', '联系方式', true),
    ('zh-TW', 'kudos-user', 'dict', 'dict', 'contact_way', '聯絡方式', true),
    ('en-US', 'kudos-user', 'dict', 'dict', 'contact_way', 'Contact Method', true),

    ('zh-CN', 'kudos-user', 'dict', 'dict', 'contact_way_status', '联系方式状态', true),
    ('zh-TW', 'kudos-user', 'dict', 'dict', 'contact_way_status', '聯絡方式狀態', true),
    ('en-US', 'kudos-user', 'dict', '', 'contact_way_status', 'Contact Method Status', true);


-- dict-item account_type
insert into "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "namespace", "key", "value", "built_in") values
    ('zh-CN', 'kudos-user', 'dict-item', 'account_type', '00', '终端用户', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'account_type', '00', '終端使用者', true),
    ('en-US', 'kudos-user', 'dict-item', 'account_type', '00', 'End User', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'account_type', '11', '租户管理员', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'account_type', '11', '租戶管理員', true),
    ('en-US', 'kudos-user', 'dict-item', 'account_type', '11', 'Tenant Administrator', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'account_type', '12', '租户成员', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'account_type', '12', '租戶成員', true),
    ('en-US', 'kudos-user', 'dict-item', 'account_type', '12', 'Tenant Member', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'account_type', '21', '门户管理员', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'account_type', '21', '門戶管理員', true),
    ('en-US', 'kudos-user', 'dict-item', 'account_type', '21', 'Portal Administrator', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'account_type', '22', '门户成员', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'account_type', '22', '門戶成員', true),
    ('en-US', 'kudos-user', 'dict-item', 'account_type', '22', 'Portal Member', true);

-- dict-item account_status
insert into "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "namespace", "key", "value", "built_in") values
    ('zh-CN', 'kudos-user', 'dict-item', 'account_status', '00', '已注销', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'account_status', '00', '已註銷', true),
    ('en-US', 'kudos-user', 'dict-item', 'account_status', '00', 'Deactivated', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'account_status', '10', '正常', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'account_status', '10', '正常', true),
    ('en-US', 'kudos-user', 'dict-item', 'account_status', '10', 'Normal', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'account_status', '20', '锁定', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'account_status', '20', '鎖定', true),
    ('en-US', 'kudos-user', 'dict-item', 'account_status', '20', 'Locked', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'account_status', '30', '账号过期', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'account_status', '30', '帳號過期', true),
    ('en-US', 'kudos-user', 'dict-item', 'account_status', '30', 'Account Expired', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'account_status', '40', '凭证过期', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'account_status', '40', '憑證過期', true),
    ('en-US', 'kudos-user', 'dict-item', 'account_status', '40', 'Credential Expired', true);

-- dict-item account_provider
insert into "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "namespace", "key", "value", "built_in") values
    ('zh-CN', 'kudos-user', 'dict-item', 'account_provider', 'google', '谷歌', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'account_provider', 'google', 'Google', true),
    ('en-US', 'kudos-user', 'dict-item', 'account_provider', 'google', 'Google', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'account_provider', 'apple', '苹果', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'account_provider', 'apple', 'Apple', true),
    ('en-US', 'kudos-user', 'dict-item', 'account_provider', 'apple', 'Apple', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'account_provider', 'wechat', '微信', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'account_provider', 'wechat', '微信', true),
    ('en-US', 'kudos-user', 'dict-item', 'account_provider', 'wechat', 'WeChat', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'account_provider', 'github', 'github', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'account_provider', 'github', 'GitHub', true),
    ('en-US', 'kudos-user', 'dict-item', 'account_provider', 'github', 'GitHub', true);

-- dict-item org_type
insert into "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "namespace", "key", "value", "built_in") values
    ('zh-CN', 'kudos-user', 'dict-item', 'org_type', '00', '总部', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'org_type', '00', '總部', true),
    ('en-US', 'kudos-user', 'dict-item', 'org_type', '00', 'Headquarters', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'org_type', '10', '分公司', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'org_type', '10', '分公司', true),
    ('en-US', 'kudos-user', 'dict-item', 'org_type', '10', 'Branch', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'org_type', '20', '事业部', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'org_type', '20', '事業部', true),
    ('en-US', 'kudos-user', 'dict-item', 'org_type', '20', 'Division', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'org_type', '30', '部门', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'org_type', '30', '部門', true),
    ('en-US', 'kudos-user', 'dict-item', 'org_type', '30', 'Department', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'org_type', '40', '小组', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'org_type', '40', '小組', true),
    ('en-US', 'kudos-user', 'dict-item', 'org_type', '40', 'Team', true);

-- dict-item contact_way
insert into "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "namespace", "key", "value", "built_in") values
    ('zh-CN', 'kudos-user', 'dict-item', 'contact_way', '101', '手机', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'contact_way', '101', '手機', true),
    ('en-US', 'kudos-user', 'dict-item', 'contact_way', '101', 'Mobile Phone', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'contact_way', '102', '固定电话', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'contact_way', '102', '固定電話', true),
    ('en-US', 'kudos-user', 'dict-item', 'contact_way', '102', 'Landline', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'contact_way', '201', 'email', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'contact_way', '201', '電子郵件', true),
    ('en-US', 'kudos-user', 'dict-item', 'contact_way', '201', 'Email', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'contact_way', '301', 'whatsapp', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'contact_way', '301', 'WhatsApp', true),
    ('en-US', 'kudos-user', 'dict-item', 'contact_way', '301', 'WhatsApp', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'contact_way', '302', 'wechat', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'contact_way', '302', 'WeChat', true),
    ('en-US', 'kudos-user', 'dict-item', 'contact_way', '302', 'WeChat', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'contact_way', '401', 'douyin', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'contact_way', '401', '抖音', true),
    ('en-US', 'kudos-user', 'dict-item', 'contact_way', '401', 'Douyin', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'contact_way', '402', 'tiktok', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'contact_way', '402', 'TikTok', true),
    ('en-US', 'kudos-user', 'dict-item', 'contact_way', '402', 'TikTok', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'contact_way', '403', 'facebook', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'contact_way', '403', 'Facebook', true),
    ('en-US', 'kudos-user', 'dict-item', 'contact_way', '403', 'Facebook', true);

-- dict-item contact_way_status
insert into "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "namespace", "key", "value", "built_in") values
    ('zh-CN', 'kudos-user', 'dict-item', 'contact_way_status', '00', '未验证', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'contact_way_status', '00', '未驗證', true),
    ('en-US', 'kudos-user', 'dict-item', 'contact_way_status', '00', 'Unverified', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'contact_way_status', '10', '正常', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'contact_way_status', '10', '正常', true),
    ('en-US', 'kudos-user', 'dict-item', 'contact_way_status', '10', 'Normal', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'contact_way_status', '20', '无法联系', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'contact_way_status', '20', '無法聯絡', true),
    ('en-US', 'kudos-user', 'dict-item', 'contact_way_status', '20', 'Unreachable', true),

    ('zh-CN', 'kudos-user', 'dict-item', 'contact_way_status', '30', '非本人联系方式', true),
    ('zh-TW', 'kudos-user', 'dict-item', 'contact_way_status', '30', '非本人聯絡方式', true),
    ('en-US', 'kudos-user', 'dict-item', 'contact_way_status', '30', 'Not Owner''s Contact', true);

--endregion DML
