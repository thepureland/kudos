-- Fixture for MsgSendServiceTest.
-- One msg_instance (template_id left null to avoid depending on msg_template) and two msg_send rows:
--   c3...001: pre-seeded success/fail counts + an idempotency key, for accumulation / lookup / status update
--   c3...002: NULL counts, to prove finishSend treats NULL as 0

merge into "msg_instance"
    ("id", "template_id", "send_type_dict_code", "event_type_dict_code", "msg_type_dict_code", "tenant_id")
values
    ('b2000000-0000-0000-0000-000000000001', null, 'auto', 'user_registered', 'welcome', 'svc-tenant-msg-send');

merge into "msg_send"
    ("id", "receiver_group_type_dict_code", "instance_id", "msg_type_dict_code", "send_status_dict_code",
     "success_count", "fail_count", "tenant_id", "idempotency_key")
values
    ('c3000000-0000-0000-0000-000000000001', 'USER', 'b2000000-0000-0000-0000-000000000001', 'welcome', '01',
     2, 1, 'svc-tenant-msg-send', 'send-idem-1'),
    ('c3000000-0000-0000-0000-000000000002', 'USER', 'b2000000-0000-0000-0000-000000000001', 'welcome', '01',
     null, null, 'svc-tenant-msg-send', null);
