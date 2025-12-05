package io.kudos.ams.sys.provider.service.impl

import io.kudos.ams.sys.common.vo.tenant.SysTenantCacheItem
import io.kudos.ams.sys.common.vo.tenant.SysTenantRecord
import io.kudos.ams.sys.common.vo.tenant.SysTenantSearchPayload
import io.kudos.ams.sys.provider.service.iservice.ISysTenantSubSystemService
import io.kudos.ams.sys.provider.cache.TenantByIdCacheHandler
import io.kudos.ams.sys.provider.cache.TenantIdsBySubSysCacheHandler
import io.kudos.ams.sys.provider.model.po.SysDataSource
import io.kudos.ams.sys.provider.model.po.SysTenantSubSystem
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import io.kudos.ams.sys.provider.service.iservice.ISysTenantService
import io.kudos.ams.sys.provider.model.po.SysTenant
import io.kudos.ams.sys.provider.dao.SysTenantDao
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service


/**
 * 租户业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysTenantService : BaseCrudService<String, SysTenant, SysTenantDao>(), ISysTenantService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this::class)

    @Autowired
    private lateinit var tenantByIdCacheHandler: TenantByIdCacheHandler

    @Autowired
    private lateinit var tenantIdsBySubSysCacheHandler: TenantIdsBySubSysCacheHandler

    @Autowired
    private lateinit var sysTenantSubSystemBiz: ISysTenantSubSystemService

    override fun getTenant(id: String): SysTenantCacheItem? {
        return tenantByIdCacheHandler.getTenantById(id)
    }

    override fun getTenants(ids: Collection<String>): Map<String, SysTenantCacheItem> {
        return tenantByIdCacheHandler.getTenantsByIds(ids)
    }

    override fun getTenants(subSysDictCode: String): List<SysTenantCacheItem> {
        val tenantIds = tenantIdsBySubSysCacheHandler.getTenantIds(subSysDictCode)
        return tenantByIdCacheHandler.getTenantsByIds(tenantIds).values.toList()
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的租户。")
        // 同步缓存
        tenantByIdCacheHandler.syncOnInsert(id)
        tenantIdsBySubSysCacheHandler.syncOnInsert(any, id)
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, SysTenant::id.name) as String
        if (success) {
            // 同步缓存
            tenantByIdCacheHandler.syncOnUpdate(id)
        } else {
            log.error("更新id为${id}的租户失败！")
        }
        return success
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val param = SysDataSource {
            this.id = id
            this.active = active
        }
        val success = dao.update(param)
        if (success) {
            // 同步缓存
            tenantByIdCacheHandler.syncOnUpdate(id)
        } else {
            log.error("更新id为${id}的租户的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(tenantId: String): Boolean {
        // 1. 先删除租户-子系统关系
        val subSystemCodes = sysTenantSubSystemBiz.searchSubSystemCodesByTenantId(tenantId)
        val criteria = Criteria.of(SysTenantSubSystem::tenantId.name, OperatorEnum.EQ, tenantId)
        val count = sysTenantSubSystemBiz.batchDeleteCriteria(criteria)
        if (count > 0) {
            // 同步缓存
            tenantIdsBySubSysCacheHandler.syncOnDelete(tenantId, subSystemCodes)
        }

        // 2. 再删除租户
        val success = super.deleteById(tenantId)
        if (success) {
            // 同步缓存
            tenantByIdCacheHandler.syncOnDelete(tenantId)
        } else {
            log.error("删除id为${tenantId}的租户失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(tenantIds: Collection<String>): Int {
        // 1.查出对应的子系统编码
        val tenantIdAndSubSysCodesMap = sysTenantSubSystemBiz.groupingSubSystemCodesByTenantIds(tenantIds)
        val subSystemCodes = tenantIdAndSubSysCodesMap.values.flatten().toSet()

        // 2.删除租户-子系统关系
        val criteria = Criteria.of(SysTenantSubSystem::tenantId.name, OperatorEnum.IN, tenantIds)
        val count = sysTenantSubSystemBiz.batchDeleteCriteria(criteria)

        // 3.删除租户
        if (count >= 0) {
            val count = super.batchDelete(tenantIds)
            log.debug("批量删除租户，期望删除${tenantIds.size}条，实际删除${count}条。")
        }

        // 3.同步缓存
        tenantByIdCacheHandler.syncOnBatchDelete(tenantIds)
        tenantIdsBySubSysCacheHandler.syncOnBatchDelete(tenantIds, subSystemCodes)
        return count
    }


    override fun getAllActiveTenants(): Map<String, List<SysTenantRecord>> {
        val searchPayload = SysTenantSearchPayload().apply { active = true }
        @Suppress("UNCHECKED_CAST")
        val records = dao.search(searchPayload) as List<SysTenantRecord>
//        return records.groupBy { it.subSysDictCode!! }
        //TODO
        return mapOf()
    }

    //endregion your codes 2

}