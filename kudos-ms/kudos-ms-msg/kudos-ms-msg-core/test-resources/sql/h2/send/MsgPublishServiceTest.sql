-- msg_template: 一条 auto 模板，供 MsgPublishServiceTest 的 publish 幂等用例匹配
-- (tenantId, eventTypeDictCode, msgTypeDictCode) = (svc-tenant-msg-idem, user_registered, welcome)
merge into "msg_template"
    ("id", "send_type_dict_code", "event_type_dict_code", "msg_type_dict_code", "receiver_group_code",
     "locale_dict_code", "title", "content", "default_active", "default_title", "default_content", "tenant_id")
values
    ('a1000000-0000-0000-0000-000000000001', 'auto', 'user_registered', 'welcome', null,
     null, 'Welcome ${name}', 'Hello ${name}, welcome!', true, 'Welcome', 'Hello, welcome!', 'svc-tenant-msg-idem');
