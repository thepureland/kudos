package io.kudos.ms.sys.core.param.service.iservice
import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.param.vo.SysParamCacheEntry
import io.kudos.ms.sys.common.param.vo.response.SysParamRow
import io.kudos.ms.sys.core.param.model.po.SysParam


/**
 * 参数业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysParamService : IBaseCrudService<String, SysParam> {

    /**
     * 按原子服务编码 + 参数名从按模块缓存读取（仅 active=true 会写入该缓存）
     */
    fun getParamFromCache(atomicServiceCode: String, paramName: String): SysParamCacheEntry?

    /**
     * 按原子服务编码直查库得到参数列表行
     */
    fun getParamsByAtomicServiceCode(atomicServiceCode: String): List<SysParamRow>

    /**
     * 读取参数取值：优先 [SysParamCacheEntry.paramValue]，其次 [SysParamCacheEntry.defaultValue]，否则 [defaultValue]
     */
    fun getParamValueFromCache(
        atomicServiceCode: String,
        paramName: String,
        defaultValue: String? = null
    ): String?

    /**
     * 更新启用状态，并同步按模块缓存
     */
    fun updateActive(id: String, active: Boolean): Boolean

}
