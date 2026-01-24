package io.kudos.ams.sys.provider.service.impl

import io.kudos.ams.sys.provider.service.iservice.ISysParamService
import io.kudos.ams.sys.provider.model.po.SysParam
import io.kudos.ams.sys.provider.dao.SysParamDao
import io.kudos.ams.sys.provider.cache.ParamByModuleAndNameCacheHandler
import io.kudos.ams.sys.common.vo.param.SysParamCacheItem
import io.kudos.ams.sys.common.vo.param.SysParamRecord
import io.kudos.ams.sys.common.vo.param.SysParamSearchPayload
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 参数业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysParamService : BaseCrudService<String, SysParam, SysParamDao>(), ISysParamService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    @Autowired
    private lateinit var paramByModuleAndNameCacheHandler: ParamByModuleAndNameCacheHandler

    override fun getParamByModuleAndName(moduleCode: String, paramName: String): SysParamCacheItem? {
        return paramByModuleAndNameCacheHandler.getParam(moduleCode, paramName)
    }

    override fun getParamsByModuleCode(moduleCode: String): List<SysParamRecord> {
        val searchPayload = SysParamSearchPayload().apply {
            this.moduleCode = moduleCode
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload) as List<SysParamRecord>
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
            paramByModuleAndNameCacheHandler.syncOnUpdateActive(id, active)
        } else {
            log.error("更新id为${id}的参数的启用状态为${active}失败！")
        }
        return success
    }

    override fun getParamValue(moduleCode: String, paramName: String, defaultValue: String?): String? {
        val param = getParamByModuleAndName(moduleCode, paramName)
        return param?.paramValue ?: param?.defaultValue ?: defaultValue
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的参数。")
        paramByModuleAndNameCacheHandler.syncOnInsert(any, id)
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, SysParam::id.name) as String
        if (success) {
            log.debug("更新id为${id}的参数。")
            paramByModuleAndNameCacheHandler.syncOnUpdate(any, id)
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
            paramByModuleAndNameCacheHandler.syncOnDelete(param, id)
        } else {
            log.error("删除id为${id}的参数失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val params = dao.inSearchById(ids)
        val moduleAndNames = params.map { Pair(it.moduleCode, it.paramName) }
        val count = super.batchDelete(ids)
        log.debug("批量删除参数，期望删除${ids.size}条，实际删除${count}条。")
        paramByModuleAndNameCacheHandler.syncOnBatchDelete(ids, moduleAndNames)
        return count
    }

    //endregion your codes 2

}