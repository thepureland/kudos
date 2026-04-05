-- user_contact_way: 每条 id 唯一，供 UserContactWayDao 用例使用
merge into "user_contact_way" ("id", "user_id", "contact_way_dict_code", "contact_way_value", "contact_way_status_dict_code", "priority", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('44444444-0000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000001', 'EML', 'user@example.com', '00', 1, 'from UserContactWayDaoTest', true, false, 'system', '系统');
