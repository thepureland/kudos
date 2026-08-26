merge into "auth_identity_provider" (
    "id", "tenant_id", "template_id", "code", "display_name", "client_id",
    "jit_policy", "link_policy", "active"
) key ("id") values (
    'e3000000-0000-0000-0000-000000000001', 'jit-config-tenant-1',
    'a0010000-0000-0000-0000-000000000003', 'jit-config-oidc', 'JIT Config OIDC', 'client-id',
    'JIT_CREATE', 'BOUND_ONLY', true
);

merge into "auth_identity_provider_jit_config" (
    "id", "tenant_id", "username_strategy", "require_verified_email", "allowed_email_domains",
    "default_timezone", "default_currency", "create_user_id", "create_reason", "create_time",
    "update_user_id", "update_reason", "update_time"
) key ("id") values (
    'e3000000-0000-0000-0000-000000000001', 'jit-config-tenant-1',
    'EMAIL_LOCAL_PART_HASHED', true, 'example.com,*.partner.example',
    'America/Argentina/Buenos_Aires', 'JPY', 'admin-1', 'Approved JIT defaults', current_timestamp,
    'admin-1', 'Approved JIT defaults', current_timestamp
);
