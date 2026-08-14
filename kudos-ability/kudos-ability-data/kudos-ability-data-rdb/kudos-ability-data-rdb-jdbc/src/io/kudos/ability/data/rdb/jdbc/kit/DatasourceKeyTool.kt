package io.kudos.ability.data.rdb.jdbc.kit

import io.kudos.ability.data.rdb.jdbc.consts.DatasourceConst

/**
 * String construction / parsing utility for dynamic data-source keys.
 *
 * The routing cache key format is `<dsKeyConfig>[::<serverCode>]::<tenantId>::<mode>`,
 * where `serverCode` is filled with [SERVER_CODE_DEFAULT] only when the original
 * config does not contain the separator. `mode` is one of `master` / `readonly`
 * (see [DatasourceConst]).
 *
 * All methods are pure string processing, side-effect free, and thread-safe.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object DatasourceKeyTool {

    /** Separator between key components (double colon). Chosen as `::` to avoid clashing with typical data-source keys (which often contain dots/dashes). */
    private const val SEPARATOR = "::"

    /** Default server code, used to fill in when the original `dsKeyConfig` does not contain a separator. */
    const val SERVER_CODE_DEFAULT: String = "default"

    /**
     * Builds the cache map key for routing resolution. Rules:
     *  - Original `dsKeyConfig` does not contain the `::` separator -> `<dsKeyConfig>::default::<tenantId>::<suffix>`
     *  - Original `dsKeyConfig` already contains the separator -> `<dsKeyConfig>::<tenantId>::<suffix>` (does not fill in default)
     */
    fun convertCacheMapKey(dsKeyConfig: String, tenantId: String?, suffix: String?): String {
        if (!dsKeyConfig.contains(SEPARATOR)) {
            return listOf(dsKeyConfig, SERVER_CODE_DEFAULT, tenantId, suffix).joinToString(SEPARATOR)
        }
        return listOf(dsKeyConfig, tenantId, suffix).joinToString(SEPARATOR)
    }

    /**
     * Parses the serverCode (position 1) out of a cache map key.
     *  - Blank input -> returns `""`
     *  - Single segment (no separator) -> returns `null` ("not configured, use default")
     *  - Multiple segments -> returns the 2nd segment
     */
    fun getServerCode(contextMapKey: String?): String? {
        //contextMapKey = _context::{serverCode}::tenantId::[master|readOnly]
        if (contextMapKey.isNullOrBlank()) {
            return ""
        }
        val parts = contextMapKey.split(SEPARATOR).dropLastWhile { it.isEmpty() }
        return if (parts.size == 1) {
            //Not configured; fall back to the default service.
            null
        } else {
            parts[1]
        }
    }

    /**
     * Returns the last segment of a cache map key, which by convention is the mode
     * (master / readonly) suffix. Returns `""` when input is blank; otherwise returns
     * the last segment string.
     */
    fun getSuffix(cacheMapKey: String?): String? {
        if (cacheMapKey.isNullOrBlank()) {
            return ""
        }
        // Return the last element ("" when every segment is empty, e.g. the input is all separators).
        return cacheMapKey.split(SEPARATOR).dropLastWhile { it.isEmpty() }.lastOrNull() ?: ""
    }

    /**
     * Determines whether a cache map key carries the "read-only replica" mode: its last segment
     * must **be** [DatasourceConst.MODE_READONLY]. A plain-suffix check would misclassify
     * ordinary data source keys that merely end with the word (e.g. `my_readonly`).
     */
    fun isReadOnly(dsKey: String): Boolean =
        dsKey == DatasourceConst.MODE_READONLY || dsKey.endsWith(SEPARATOR + DatasourceConst.MODE_READONLY)
}
