package io.kudos.ability.data.rdb.jdbc.kit

import io.kudos.ability.data.rdb.jdbc.consts.DatasourceConst

/**
 * 动态数据源 key 的字符串构造 / 解析工具。
 *
 * 约定路由 cache key 的组装格式 `<dsKeyConfig>[::<serverCode>]::<tenantId>::<mode>`，
 * 其中 `serverCode` 仅在原始配置不含分隔符时由 [SERVER_CODE_DEFAULT] 填补。`mode` 是
 * `master` / `readonly` 之一（见 [DatasourceConst]）。
 *
 * 所有方法纯字符串处理，无副作用、线程安全。
 *
 * @author K
 * @since 1.0.0
 */
object DatasourceKeyTool {

    /** key 各分量之间的分隔符（双冒号）。选 `::` 避免与典型的数据源 key（含点 / 短横）冲突。 */
    private const val SEPERATOR = "::"

    /** 默认服务编码，当原始 `dsKeyConfig` 不含分隔符时用它补位。 */
    const val SERVER_CODE_DEFAULT: String = "default"

    /**
     * 构造路由解析的 cache map key。规则：
     *  - 原始 `dsKeyConfig` 不含 `::` 分隔符 → `<dsKeyConfig>::default::<tenantId>::<suffix>`
     *  - 原始 `dsKeyConfig` 已含分隔符 → `<dsKeyConfig>::<tenantId>::<suffix>`（不再补 default）
     */
    fun convertCacheMapKey(dsKeyConfig: String, tenantId: String?, suffix: String?): String {
        if (!dsKeyConfig.contains(SEPERATOR)) {
            return listOf(dsKeyConfig, SERVER_CODE_DEFAULT, tenantId, suffix).joinToString(SEPERATOR)
        }
        return listOf(dsKeyConfig, tenantId, suffix).joinToString(SEPERATOR)
    }

    /**
     * 从 cache map key 反解出 serverCode（位置 1）。
     *  - 入参为空 → 返回 `""`
     *  - 单段（没分隔符）→ 返回 `null`（"没配置，走默认"）
     *  - 多段 → 返回第 2 段
     */
    fun getServerCode(contextMapKey: String?): String? {
        //contextMapKey = _context::{serverCode}::tenantId::[master|readOnly]
        if (contextMapKey.isNullOrBlank()) {
            return ""
        }
        val parts: Array<String?> =
            contextMapKey.split(SEPERATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return if (parts.size == 1) {
            //没配置，获取默认的服务
            null
        } else {
            parts[1]
        }
    }

    /**
     * 取 cache map key 的最后一段，约定是 mode（master / readonly）后缀。
     * 入参为空返回 `""`；其它情况返回最后一段字符串。
     */
    fun getSuffix(cacheMapKey: String?): String? {
        if (cacheMapKey.isNullOrBlank()) {
            return ""
        }
        val parts: Array<String?> =
            cacheMapKey.split(SEPERATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        // 取最后一个元素
        return if (parts.isNotEmpty()) parts[parts.size - 1] else ""
    }

    /** 判断 dsKey 是否为"只读副本"（后缀 [DatasourceConst.MODE_READONLY]）。 */
    fun isReadOnly(dsKey: String): Boolean {
        return dsKey.endsWith(DatasourceConst.MODE_READONLY)
    }
}
