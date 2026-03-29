package io.kudos.ms.sys.common.api

import io.kudos.ms.sys.common.vo.system.SysSystemCacheEntry


/**
 * 系统 对外API
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysSystemApi {

    fun getSystemFromCache(code: String): SysSystemCacheEntry?

    fun getAllSystemsFromCache(): List<SysSystemCacheEntry>

    fun getSystemsExcludeSubSystemFromCache(): List<SysSystemCacheEntry>

    fun updateActive(code: String, active: Boolean): Boolean

    fun getSubSystemsFromCache(systemCode: String): List<SysSystemCacheEntry>


}
