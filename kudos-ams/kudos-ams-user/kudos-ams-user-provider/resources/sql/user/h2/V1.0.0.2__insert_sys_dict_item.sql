--region DML

merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "active", "built_in")
    values ('e8ff3f9a-a57a-4183-item-provider0001', '68139ed2-dict-user-acct-provider0000', 'google', '谷歌', 1, null, true, true),
    ('e8ff3f9a-a57a-4183-item-provider0002', '68139ed2-dict-user-acct-provider0000', 'apple', '苹果', 1, null, true, true),
    ('e8ff3f9a-a57a-4183-item-provider0003', '68139ed2-dict-user-acct-provider0000', 'wechat', '微信', 1, null, true, true),
    ('e8ff3f9a-a57a-4183-item-provider0004', '68139ed2-dict-user-acct-provider0000', 'github', 'github', 1, null, true, true);

--endregion DML