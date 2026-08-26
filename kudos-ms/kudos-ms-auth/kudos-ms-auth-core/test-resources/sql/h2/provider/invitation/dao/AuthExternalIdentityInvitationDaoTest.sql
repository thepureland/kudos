merge into "auth_identity_provider" (
    "id", "tenant_id", "template_id", "code", "display_name", "client_id",
    "jit_policy", "link_policy", "active"
) key ("id") values (
    'e1000000-0000-0000-0000-000000000001', 'invite-dao-tenant-1',
    'a0010000-0000-0000-0000-000000000001', 'invite-google', 'Invite Google', 'client-id',
    'INVITE_ONLY', 'BOUND_ONLY', true
);

merge into "auth_external_identity_invitation" (
    "id", "tenant_id", "user_id", "identity_provider_id", "token_hash",
    "max_uses", "used_count", "expires_at", "active", "create_user_id",
    "create_reason", "create_time", "update_time"
) key ("id") values (
    'e2000000-0000-0000-0000-000000000001', 'invite-dao-tenant-1',
    'e3000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001',
    'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
    1, 0, timestamp '2099-01-01 00:00:00', true, 'admin-1',
    'Approved onboarding', current_timestamp, current_timestamp
);
