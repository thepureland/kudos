merge into "auth_identity_provider" (
    "id", "tenant_id", "template_id", "code", "display_name", "issuer", "client_id",
    "jit_policy", "link_policy", "active", "create_user_id", "create_reason", "create_time",
    "update_user_id", "update_reason", "update_time"
) key ("id") values (
    'e4000000-0000-0000-0000-000000000001', 'claim-map-tenant-1',
    'a0010000-0000-0000-0000-000000000003', 'enterprise-main', 'Enterprise OAuth',
    'https://identity.example.com', 'client-id', 'DISABLED', 'BOUND_ONLY', true,
    'admin-1', 'Register enterprise IdP', current_timestamp,
    'admin-1', 'Register enterprise IdP', current_timestamp
);

merge into "auth_identity_provider_claim_mapping" (
    "id", "tenant_id", "subject_claims", "username_claims", "display_name_claims",
    "email_claims", "email_verified_claims", "avatar_claims", "create_user_id", "create_reason",
    "create_time", "update_user_id", "update_reason", "update_time"
) key ("id") values (
    'e4000000-0000-0000-0000-000000000001', 'claim-map-tenant-1',
    'identity.stable_id', 'profile.username,login', 'profile.display_name',
    'profiles.0.email,email', 'flags.email_verified', 'profile.avatar.url',
    'admin-1', 'Initial claim contract', current_timestamp,
    'admin-2', 'Approve claim contract', current_timestamp
);
