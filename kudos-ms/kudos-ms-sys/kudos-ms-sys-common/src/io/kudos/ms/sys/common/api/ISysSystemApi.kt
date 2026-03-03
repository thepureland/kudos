package io.kudos.ms.sys.common.api

import io.kudos.ms.sys.common.vo.system.SysSystemCacheItem
import io.kudos.ms.sys.common.vo.system.SysSystemRecord


/**
 * 系统 对外API
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysSystemApi {
//endregion your codes 1

    //region your codes 2

    fun getSystemByCode(code: String): SysSystemCacheItem?

    fun getAllActiveSystems(): List<SysSystemRecord>

    fun updateActive(code: String, active: Boolean): Boolean

    fun getSubSystemsBySystemCode(systemCode: String): List<SysSystemRecord>

    //endregion your codes 2

}