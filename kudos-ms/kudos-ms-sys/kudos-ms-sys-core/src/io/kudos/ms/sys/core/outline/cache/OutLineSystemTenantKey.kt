package io.kudos.ms.sys.core.outline.cache

import io.kudos.context.support.Consts

/**
 * 出网白名单缓存的「系统编码 + 租户维度」编码统一规范。
 *
 * 库表中平台级出网规则的 `tenant_id` 取 `NULL`；缓存层一律以空串表示平台级。
 */
internal object OutLineSystemTenantKey {

    /** 归一化租户编码：`null` / 空白统一映射为空串（平台级）。 */
    fun normalize(tenantId: String?): String = tenantId?.trim()?.takeIf { it.isNotEmpty() } ?: ""

    /** 拼出「systemCode + 归一化 tenantId」复合 key。 */
    fun compositeKey(systemCode: String, tenantId: String?): String =
        "${systemCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${normalize(tenantId)}"
}
