package io.kudos.ms.sys.core.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.vo.param.SysParamCacheEntry
import io.kudos.ms.sys.common.vo.param.response.SysParamRow
import io.kudos.ms.sys.core.cache.ParamByModuleAndNameCache
import io.kudos.ms.sys.core.dao.SysParamDao
import io.kudos.ms.sys.core.model.po.SysParam
import io.kudos.ms.sys.core.service.iservice.ISysParamService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass


/**
 * 参数业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class SysParamService(
    dao: SysParamDao
) : BaseCrudService<String, SysParam, SysParamDao>(dao), ISysParamService {

    private val log = LogFactory.getLog(this::class)

    @Autowired
    private lateinit var paramByModuleAndNameCache: ParamByModuleAndNameCache

    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        return if (returnType == SysParamCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            dao.get(id, SysParamCacheEntry::class) as R?
        } else {
            super.get(id, returnType)
        }
    }

    override fun getParamFromCache(atomicServiceCode: String, paramName: String): SysParamCacheEntry? {
        return paramByModuleAndNameCache.getParam(atomicServiceCode, paramName)
    }

    override fun getParamsByAtomicServiceCode(atomicServiceCode: String): List<SysParamRow> {
        val criteria = Criteria(SysParam::atomicServiceCode eq atomicServiceCode)
        return dao.searchAs<SysParamRow>(criteria)
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val param = SysParam {
            this.id = id
            this.active = active
        }
        val success = dao.update(param)
        if (success) {
            log.debug("更新id为${id}的参数的启用状态为${active}。")
            paramByModuleAndNameCache.syncOnUpdateActive(id, active)
        } else {
            log.error("更新id为${id}的参数的启用状态为${active}失败！")
        }
        return success
    }

    override fun getParamValueFromCache(atomicServiceCode: String, paramName: String, defaultValue: String?): String? {
        val param = getParamFromCache(atomicServiceCode, paramName)
        return param?.paramValue ?: param?.defaultValue ?: defaultValue
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的参数。")
        paramByModuleAndNameCache.syncOnInsert(any, id)
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, SysParam::id.name) as String
        if (success) {
            log.debug("更新id为${id}的参数。")
            paramByModuleAndNameCache.syncOnUpdate(any, id)
        } else {
            log.error("更新id为${id}的参数失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val param = dao.get(id)
        if (param == null) {
            log.warn("删除id为${id}的参数时，发现其已不存在！")
            return false
        }
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除id为${id}的参数。")
            paramByModuleAndNameCache.syncOnDelete(param, id)
        } else {
            log.error("删除id为${id}的参数失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val params = dao.inSearchById(ids)
        val moduleAndNames = params.map { Pair(it.atomicServiceCode, it.paramName) }
        val count = super.batchDelete(ids)
        log.debug("批量删除参数，期望删除${ids.size}条，实际删除${count}条。")
        paramByModuleAndNameCache.syncOnBatchDelete(ids, moduleAndNames)
        return count
    }

}
