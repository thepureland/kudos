--region DML

-- account_type
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "active", "built_in")
    values
    ('e8ff3f9a-a57a-4183-acct-type00000001', '68139ed2-dict-user-acct-type00000000', '00', '终端用户', 1, null, true, true),
    ('e8ff3f9a-a57a-4183-acct-type00000002', '68139ed2-dict-user-acct-type00000000', '11', '租户管理员', 2, null, true, true),
    ('e8ff3f9a-a57a-4183-acct-type00000003', '68139ed2-dict-user-acct-type00000000', '12', '租户成员', 3, null, true, true),
    ('e8ff3f9a-a57a-4183-acct-type00000004', '68139ed2-dict-user-acct-type00000000', '21', '门户管理员', 4, null, true, true),
    ('e8ff3f9a-a57a-4183-acct-type00000005', '68139ed2-dict-user-acct-type00000000', '22', '门户成员', 5, null, true, true);


-- account_status
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "active", "built_in")
    values
    ('e8ff3f9a-a57a-4183-acct-status000001', '68139ed2-dict-user-acct-status000000', '00', '已注销', 1, null, true, true),
    ('e8ff3f9a-a57a-4183-acct-status000002', '68139ed2-dict-user-acct-status000000', '10', '正常', 2, null, true, true),
    ('e8ff3f9a-a57a-4183-acct-status000003', '68139ed2-dict-user-acct-status000000', '20', '锁定', 3, null, true, true),
    ('e8ff3f9a-a57a-4183-acct-status000004', '68139ed2-dict-user-acct-status000000', '30', '账号过期', 4, null, true, true),
    ('e8ff3f9a-a57a-4183-acct-status000005', '68139ed2-dict-user-acct-status000000', '40', '凭证过期', 5, null, true, true);

-- account_provider
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "active", "built_in")
    values
    ('e8ff3f9a-a57a-4183-item-provider0001', '68139ed2-dict-user-acct-provider0000', 'google', '谷歌', 1, null, true, true),
    ('e8ff3f9a-a57a-4183-item-provider0002', '68139ed2-dict-user-acct-provider0000', 'apple', '苹果', 2, null, true, true),
    ('e8ff3f9a-a57a-4183-item-provider0003', '68139ed2-dict-user-acct-provider0000', 'wechat', '微信', 3, null, true, true),
    ('e8ff3f9a-a57a-4183-item-provider0004', '68139ed2-dict-user-acct-provider0000', 'github', 'github', 4, null, true, true);

-- org_type
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "active", "built_in")
    values
    ('e8ff3f9a-a57a-4183-0org-type00000001', '68139ed2-dict-user-0org-type00000000', '00', '总部', 1, null, true, false),
    ('e8ff3f9a-a57a-4183-0org-type00000002', '68139ed2-dict-user-0org-type00000000', '10', '分公司', 2, null, true, false),
    ('e8ff3f9a-a57a-4183-0org-type00000003', '68139ed2-dict-user-0org-type00000000', '20', '事业部', 3, null, true, false),
    ('e8ff3f9a-a57a-4183-0org-type00000004', '68139ed2-dict-user-0org-type00000000', '30', '部门', 4, null, true, false),
    ('e8ff3f9a-a57a-4183-0org-type00000005', '68139ed2-dict-user-0org-type00000000', '40', '小组', 5, null, true, false);

-- contact_way
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "active", "built_in")
    values
    ('e8ff3f9a-a57a-4183-item-contactway01', '68139ed2-dict-user-0000-contactway00', '101', '手机', 1, null, true, true),
    ('e8ff3f9a-a57a-4183-item-contactway02', '68139ed2-dict-user-0000-contactway00', '102', '固定电话', 2, null, true, true),
    ('e8ff3f9a-a57a-4183-item-contactway03', '68139ed2-dict-user-0000-contactway00', '201', 'email', 3, null, true, true),
    ('e8ff3f9a-a57a-4183-item-contactway04', '68139ed2-dict-user-0000-contactway00', '301', 'whatsapp', 4, null, true, false),
    ('e8ff3f9a-a57a-4183-item-contactway05', '68139ed2-dict-user-0000-contactway00', '302', 'wechat', 5, null, true, false),
    ('e8ff3f9a-a57a-4183-item-contactway06', '68139ed2-dict-user-0000-contactway00', '401', 'douyin', 6, null, true, false),
    ('e8ff3f9a-a57a-4183-item-contactway07', '68139ed2-dict-user-0000-contactway00', '402', 'tiktok', 7, null, true, false),
    ('e8ff3f9a-a57a-4183-item-contactway08', '68139ed2-dict-user-0000-contactway00', '403', 'facebook', 8, null, true, false);

-- contact_way_status
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "active", "built_in")
    values
    ('e8ff3f9a-a57a-4183-stas-contactway01', '68139ed2-dict-user-0000-contactwayst', '00', '未验证', 1, null, true, true),
    ('e8ff3f9a-a57a-4183-stas-contactway02', '68139ed2-dict-user-0000-contactwayst', '10', '正常', 2, null, true, true),
    ('e8ff3f9a-a57a-4183-stas-contactway03', '68139ed2-dict-user-0000-contactwayst', '20', '无法联系', 3, null, true, true),
    ('e8ff3f9a-a57a-4183-stas-contactway04', '68139ed2-dict-user-0000-contactwayst', '30', '非本人联系方式', 4, null, true, true);

--endregion DML