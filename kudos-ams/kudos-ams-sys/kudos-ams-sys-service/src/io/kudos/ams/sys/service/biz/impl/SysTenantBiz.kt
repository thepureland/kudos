package io.kudos.ams.sys.service.biz.impl

import io.kudos.ams.sys.common.vo.tenant.SysTenantCacheItem
import io.kudos.ams.sys.common.vo.tenant.SysTenantRecord
import io.kudos.ams.sys.common.vo.tenant.SysTenantSearchPayload
import io.kudos.ams.sys.service.biz.ibiz.ISysTenantSubSystemBiz
import io.kudos.ams.sys.service.cache.TenantByIdCacheHandler
import io.kudos.ams.sys.service.cache.TenantsBySubSysCacheHandler
import io.kudos.ams.sys.service.model.po.SysDataSource
import io.kudos.ams.sys.service.model.po.SysTenantSubSystem
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import io.kudos.ams.sys.service.biz.ibiz.ISysTenantBiz
import io.kudos.ams.sys.service.model.po.SysTenant
import io.kudos.ams.sys.service.dao.SysTenantDao
import io.kudos.ability.data.rdb.ktorm.biz.BaseCrudBiz
import org.springframework.stereotype.Service


/**
 * 租户业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysTenantBiz : BaseCrudBiz<String, SysTenant, SysTenantDao>(), ISysTenantBiz {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this::class)

    @Autowired
    private lateinit var tenantByIdCacheHandler: TenantByIdCacheHandler

    @Autowired
    private lateinit var tenantBySubSysCacheHandler: TenantsBySubSysCacheHandler

    @Autowired
    private lateinit var sysTenantSubSystemBiz: ISysTenantSubSystemBiz

    override fun getTenant(id: String): SysTenantCacheItem? {
        return tenantByIdCacheHandler.getTenantById(id)
    }

    override fun getTenants(ids: Collection<String>): Map<String, SysTenantCacheItem> {
        return tenantByIdCacheHandler.getTenantsByIds(ids)
    }

    override fun getTenants(subSysDictCode: String): List<SysTenantCacheItem> {
        return tenantBySubSysCacheHandler.getTenantsFromCache(subSysDictCode)
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的租户。")
        // 同步缓存
        tenantByIdCacheHandler.syncOnInsert(id)
        tenantBySubSysCacheHandler.syncOnInsert(any, id)
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, SysTenant::id.name) as String
        if (success) {
            // 同步缓存
            tenantByIdCacheHandler.syncOnUpdate(id)
            tenantBySubSysCacheHandler.syncOnUpdate(any, id)
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
            tenantBySubSysCacheHandler.syncOnUpdate(null, id)
        } else {
            log.error("更新id为${id}的租户的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val sysTenant = tenantByIdCacheHandler.getTenantById(id)!!
        val success = super.deleteById(id)
        if (success) {
            // 同步缓存
            tenantByIdCacheHandler.syncOnDelete(id)
            tenantBySubSysCacheHandler.syncOnDelete(sysTenant)
        } else {
            log.error("删除id为${id}的租户失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        // 1.查出对应的子系统编码
        val tenantIdAndSubSysCodesMap = sysTenantSubSystemBiz.groupingSubSystemCodesByTenantIds(ids)
        val subSystemCodes = tenantIdAndSubSysCodesMap.values.flatten().toSet()

        // 1.删除租户-子系统关系
        val criteria = Criteria.add(SysTenantSubSystem::tenantId.name, OperatorEnum.IN, ids)
        val count = sysTenantSubSystemBiz.batchDeleteCriteria(criteria)

        // 2.删除租户
        if (count >= 0) {
            val count = super.batchDelete(ids)
            log.debug("批量删除租户，期望删除${ids.size}条，实际删除${count}条。")
        }

        // 3.同步缓存
        tenantByIdCacheHandler.syncOnBatchDelete(ids)
        tenantBySubSysCacheHandler.syncOnBatchDelete(ids, subSystemCodes)
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