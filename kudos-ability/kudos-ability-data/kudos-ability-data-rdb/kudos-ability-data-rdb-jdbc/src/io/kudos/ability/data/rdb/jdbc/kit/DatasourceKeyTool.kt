package io.kudos.ability.data.rdb.jdbc.kit

import io.kudos.ability.data.rdb.jdbc.consts.DatasourceConst


object DatasourceKeyTool {
    private const val SEPERATOR = "::"
    const val SERVER_CODE_DEFAULT: String = "default"

    fun convertCacheMapKey(dsKeyConfig: String, tenantId: String?, suffix: String?): String {
        if (!dsKeyConfig.contains(SEPERATOR)) {
            return listOf(dsKeyConfig, SERVER_CODE_DEFAULT, tenantId, suffix).joinToString(SEPERATOR)
        }
        return listOf(dsKeyConfig, tenantId, suffix).joinToString()
    }

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

    fun getSuffix(cacheMapKey: String?): String? {
        if (cacheMapKey.isNullOrBlank()) {
            return ""
        }
        val parts: Array<String?> =
            cacheMapKey.split(SEPERATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        // 取最后一个元素
        return if (parts.isNotEmpty()) parts[parts.size - 1] else ""
    }

    fun isReadOnly(dsKey: String): Boolean {
        return dsKey.endsWith(DatasourceConst.MODE_READONLY)
    }
}
