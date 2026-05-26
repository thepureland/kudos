package io.kudos.ms.sys.core.param.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.param.vo.SysParamCacheEntry
import io.kudos.ms.sys.common.param.vo.response.SysParamRow
import io.kudos.ms.sys.core.param.model.po.SysParam


/**
 * Parameter business interface.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysParamService : IBaseCrudService<String, SysParam> {

    /**
     * Read from the by-module cache using atomic service code + parameter name (only active=true entries are written into this cache).
     */
    fun getParamFromCache(atomicServiceCode: String, paramName: String): SysParamCacheEntry?

    /**
     * Query the parameter list rows directly from the DB by atomic service code.
     */
    fun getParamsByAtomicServiceCode(atomicServiceCode: String): List<SysParamRow>

    /**
     * Read the parameter value: prefer [SysParamCacheEntry.paramValue], then [SysParamCacheEntry.defaultValue], otherwise [defaultValue].
     */
    fun getParamValueFromCache(
        atomicServiceCode: String,
        paramName: String,
        defaultValue: String? = null
    ): String?

    /**
     * Update active state and sync the by-module cache.
     */
    fun updateActive(id: String, active: Boolean): Boolean

}
