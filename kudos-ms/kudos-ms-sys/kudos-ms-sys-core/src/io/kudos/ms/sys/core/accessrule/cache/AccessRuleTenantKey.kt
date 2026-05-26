package io.kudos.ms.sys.core.accessrule.cache

import io.kudos.context.support.Consts

/**
 * Unified encoding convention for the "tenant dimension" used by access-rule-related caches.
 *
 * In the DB table, platform-level access rules have `tenant_id = NULL`;
 * the cache layer (Hash secondary index / KV key) cannot and should not carry `null`, so **empty string**
 * is used uniformly as the platform-level value. All public APIs for access-rule caches accept `String?`,
 * normalized to `""` by this utility; magic values like the literal `"null"` must not appear upstream.
 */
internal object AccessRuleTenantKey {

    /** Normalize a tenant code: trimmed blank or `null` is treated as platform-level and returns empty string. */
    fun normalize(tenantId: String?): String = tenantId?.trim()?.takeIf { it.isNotEmpty() } ?: ""

    /** Build the composite key used by KV caches: "systemCode + normalized tenantId". */
    fun compositeKey(systemCode: String, tenantId: String?): String =
        "${systemCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${normalize(tenantId)}"
}
