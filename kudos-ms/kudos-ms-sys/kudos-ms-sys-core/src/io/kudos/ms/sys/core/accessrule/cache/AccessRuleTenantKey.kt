package io.kudos.ms.sys.core.accessrule.cache

import io.kudos.context.support.Consts

/**
 * 访问规则相关缓存的「租户维度」编码统一规范。
 *
 * 库表中平台级访问规则的 `tenant_id` 取 `NULL`；
 * 缓存层（Hash 副属性索引 / KV key）不能也不应承载 `null`，因此一律以**空串**作为平台级取值。
 * 所有访问规则相关缓存的对外 API 接受 `String?`，由本工具统一归一为 `""`，禁止上游再出现 `"null"` 字面量等魔法值。
 */
internal object AccessRuleTenantKey {

    /** 归一化租户编码：去空白后空字符串/`null` 一律视为平台级，返回空串。 */
    fun normalize(tenantId: String?): String = tenantId?.trim()?.takeIf { it.isNotEmpty() } ?: ""

    /** 拼出 KV 缓存使用的「systemCode + 归一化 tenantId」复合 key。 */
    fun compositeKey(systemCode: String, tenantId: String?): String =
        "${systemCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${normalize(tenantId)}"
}
