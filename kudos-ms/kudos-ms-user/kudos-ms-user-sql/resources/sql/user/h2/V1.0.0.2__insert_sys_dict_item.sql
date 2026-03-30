--region DML

-- account_type
insert into "sys_dict_item" ("dict_id", "item_code", "item_name", "order_num", "remark", "active", "built_in") values
    ('68139ed2-dict-user-acct-type00000000', '00', 'account_type.00', 1, '终端用户', true, true),
    ('68139ed2-dict-user-acct-type00000000', '11', 'account_type.11', 2, '租户管理员', true, true),
    ('68139ed2-dict-user-acct-type00000000', '12', 'account_type.12', 3, '租户成员', true, true),
    ('68139ed2-dict-user-acct-type00000000', '21', 'account_type.21', 4, '门户管理员', true, true),
    ('68139ed2-dict-user-acct-type00000000', '22', 'account_type.22', 5, '门户成员', true, true);

-- account_status
insert into "sys_dict_item" ("dict_id", "item_code", "item_name", "order_num", "remark", "active", "built_in") values
    ('68139ed2-dict-user-acct-status000000', '00', 'account_status.00', 1, '已注销', true, true),
    ('68139ed2-dict-user-acct-status000000', '10', 'account_status.10', 2, '正常', true, true),
    ('68139ed2-dict-user-acct-status000000', '20', 'account_status.20', 3, '锁定', true, true),
    ('68139ed2-dict-user-acct-status000000', '30', 'account_status.30', 4, '账号过期', true, true),
    ('68139ed2-dict-user-acct-status000000', '40', 'account_status.40', 5, '凭证过期', true, true);

-- account_provider
insert into "sys_dict_item" ("dict_id", "item_code", "item_name", "order_num", "remark", "active", "built_in") values
    ('68139ed2-dict-user-acct-provider0000', 'google', 'account_provider.google', 1, '谷歌', true, true),
    ('68139ed2-dict-user-acct-provider0000', 'apple', 'account_provider.apple', 2, '苹果', true, true),
    ('68139ed2-dict-user-acct-provider0000', 'wechat', 'account_provider.wechat', 3, '微信', true, true),
    ('68139ed2-dict-user-acct-provider0000', 'github', 'account_provider.github', 4, 'github', true, true);

-- org_type
insert into "sys_dict_item" ("dict_id", "item_code", "item_name", "order_num", "remark", "active", "built_in") values
    ('68139ed2-dict-user-0org-type00000000', '00', 'org_type.00', 1, '总部', true, false),
    ('68139ed2-dict-user-0org-type00000000', '10', 'org_type.10', 2, '分公司', true, false),
    ('68139ed2-dict-user-0org-type00000000', '20', 'org_type.20', 3, '事业部', true, false),
    ('68139ed2-dict-user-0org-type00000000', '30', 'org_type.30', 4, '部门', true, false),
    ('68139ed2-dict-user-0org-type00000000', '40', 'org_type.40', 5, '小组', true, false);

-- contact_way
insert into "sys_dict_item" ("dict_id", "item_code", "item_name", "order_num", "remark", "active", "built_in") values
    ('68139ed2-dict-user-0000-contactway00', '101', 'contact_way.101', 1, '手机', true, true),
    ('68139ed2-dict-user-0000-contactway00', '102', 'contact_way.102', 2, '固定电话', true, true),
    ('68139ed2-dict-user-0000-contactway00', '201', 'contact_way.201', 3, 'email', true, true),
    ('68139ed2-dict-user-0000-contactway00', '301', 'contact_way.301', 4, 'whatsapp', true, false),
    ('68139ed2-dict-user-0000-contactway00', '302', 'contact_way.302', 5, 'wechat', true, false),
    ('68139ed2-dict-user-0000-contactway00', '401', 'contact_way.401', 6, 'douyin', true, false),
    ('68139ed2-dict-user-0000-contactway00', '402', 'contact_way.402', 7, 'tiktok', true, false),
    ('68139ed2-dict-user-0000-contactway00', '403', 'contact_way.403', 8, 'facebook', true, false);

-- contact_way_status
insert into "sys_dict_item" ("dict_id", "item_code", "item_name", "order_num", "remark", "active", "built_in") values
    ('68139ed2-dict-user-0000-contactwayst', '00', 'contact_way_status.00', 1, '未验证', true, true),
    ('68139ed2-dict-user-0000-contactwayst', '10', 'contact_way_status.10', 2, '正常', true, true),
    ('68139ed2-dict-user-0000-contactwayst', '20', 'contact_way_status.20', 3, '无法联系', true, true),
    ('68139ed2-dict-user-0000-contactwayst', '30', 'contact_way_status.30', 4, '非本人联系方式', true, true);

--endregion DML
