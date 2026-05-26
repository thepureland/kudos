package io.kudos.ms.sys.core.outline.cache

import io.kudos.context.support.Consts

/**
 * Unified "system code + tenant dimension" key convention for the outbound whitelist cache.
 *
 * Platform-level outbound rules have `tenant_id = NULL` in the database; in the cache layer they are always represented by an empty string.
 */
internal object OutLineSystemTenantKey {

    /** Normalize tenant code: `null` / blank are mapped to an empty string (platform level). */
    fun normalize(tenantId: String?): String = tenantId?.trim()?.takeIf { it.isNotEmpty() } ?: ""

    /** Build the composite key "systemCode + normalized tenantId". */
    fun compositeKey(systemCode: String, tenantId: String?): String =
        "${systemCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${normalize(tenantId)}"
}
