-- Fixture for MsgTemplateServiceTest.
-- Same (tenant, event, msgType) with different locales, plus one other event, to exercise
-- the optional-locale matching of getTemplateByEvent.
--   f1: locale zh-CN   f2: locale en-US   f3: locale null   (all event=evt_login / msgType=email)
--   f4: different event (evt_logout)

merge into "msg_template"
    ("id", "send_type_dict_code", "event_type_dict_code", "msg_type_dict_code", "locale_dict_code",
     "title", "content", "default_active", "tenant_id")
values
    ('f1000000-0000-0000-0000-000000000001', 'auto', 'evt_login', 'email', 'zh-CN',
     '登录-中文', '内容zh', true, 'svc-tenant-msg-tmpl'),
    ('f1000000-0000-0000-0000-000000000002', 'auto', 'evt_login', 'email', 'en-US',
     'Login-EN', 'content-en', true, 'svc-tenant-msg-tmpl'),
    ('f1000000-0000-0000-0000-000000000003', 'auto', 'evt_login', 'email', null,
     'Login-default', 'content-default', true, 'svc-tenant-msg-tmpl'),
    ('f1000000-0000-0000-0000-000000000004', 'auto', 'evt_logout', 'email', null,
     'Logout-default', 'content-logout', true, 'svc-tenant-msg-tmpl');
