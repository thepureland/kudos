package io.kudos.ms.auth.common.provider.enums

/** How an unbound external principal may obtain a local account. */
enum class ExternalJitPolicyEnum {
    DISABLED,
    INVITE_ONLY,
    JIT_CREATE,
    ADMIN_PREPROVISIONED,
}

/** How an external principal may be linked to an existing account. */
enum class ExternalLinkPolicyEnum {
    BOUND_ONLY,
    MATCH_VERIFIED_EMAIL,
    MANUAL_CONFIRM,
}

/** How a JIT-created local username obtains its readable prefix. Identity uniqueness always uses a hash suffix. */
enum class ExternalJitUsernameStrategyEnum {
    EXTERNAL_USERNAME_HASHED,
    EMAIL_LOCAL_PART_HASHED,
    OPAQUE_HASHED,
}
