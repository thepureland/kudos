-- user_account: 每条 id 唯一，供联系方式关联
merge into "user_account" ("id", "username", "tenant_id", "login_password", "display_name", "supervisor_id", "remark", "active", "built_in") values
    ('7a1a0000-0000-0000-0000-000000000001', 'contact_user1', 'tenant-contact-1', 'password', '联系用户1', '00000000-0000-0000-0000-000000000000', '测试用户1', true, false),
    ('7a1a0000-0000-0000-0000-000000000002', 'contact_user2', 'tenant-contact-1', 'password', '联系用户2', '00000000-0000-0000-0000-000000000000', '测试用户2', true, false),
    ('7a1a0000-0000-0000-0000-000000000003', 'contact_user3', 'tenant-contact-1', 'password', '联系用户3', '00000000-0000-0000-0000-000000000000', '测试用户3', true, false);

-- user_contact_way: 每条 id 唯一，user_id 关联 user_account
merge into "user_contact_way" ("id", "user_id", "contact_way_dict_code", "contact_way_value", "contact_way_status_dict_code", "priority", "remark", "active", "built_in") values
    ('8b1a0000-0000-0000-0000-000000000001', '7a1a0000-0000-0000-0000-000000000001', '01', '13300000001', '00', 1, '主手机号', true, false),
    ('8b1a0000-0000-0000-0000-000000000002', '7a1a0000-0000-0000-0000-000000000001', '01', '13300000002', '00', 2, '备用手机号', true, false),
    ('8b1a0000-0000-0000-0000-000000000003', '7a1a0000-0000-0000-0000-000000000001', '02', 'u1@example.com', '00', 3, '备用邮箱', false, false),
    ('8b1a0000-0000-0000-0000-000000000004', '7a1a0000-0000-0000-0000-000000000002', '01', '13300000003', '00', 1, '主手机号', true, false),
    ('8b1a0000-0000-0000-0000-000000000005', '7a1a0000-0000-0000-0000-000000000002', '02', 'u2@example.com', '00', 2, '备用邮箱', true, false);
