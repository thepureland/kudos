package io.kudos.ams.sys.core.service.impl

import io.kudos.ams.sys.common.vo.tenant.SysTenantCacheItem
import io.kudos.ams.sys.common.vo.tenant.SysTenantRecord
import io.kudos.ams.sys.common.vo.tenant.SysTenantSearchPayload
import io.kudos.ams.sys.core.service.iservice.ISysTenantSubSystemService
import io.kudos.ams.sys.core.cache.TenantByIdCacheHandler
import io.kudos.ams.sys.core.cache.TenantIdsBySubSysCacheHandler
import io.kudos.ams.sys.core.model.po.SysTenantSubSystem
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import io.kudos.ams.sys.core.service.iservice.ISysTenantService
import io.kudos.ams.sys.core.model.po.SysTenant
import io.kudos.ams.sys.core.dao.SysTenantDao
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service


/**
 * 租户业务
 *
 * @author K
 * @author AI: Cursor
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
        val tenant = SysTenant {
            this.id = id
            this.active = active
        }
        val success = dao.update(tenant)
        if (success) {
            log.debug("更新id为${id}的租户的启用状态为${active}。")
            // 同步缓存
            tenantByIdCacheHandler.syncOnUpdate(id)
        } else {
            log.error("更新id为${id}的租户的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        // 1. 先删除租户-子系统关系
        val subSystemCodes = sysTenantSubSystemBiz.searchSubSystemCodesByTenantId(id)
        val criteria = Criteria.of(SysTenantSubSystem::tenantId.name, OperatorEnum.EQ, id)
        val count = sysTenantSubSystemBiz.batchDeleteCriteria(criteria)
        if (count > 0) {
            // 同步缓存
            tenantIdsBySubSysCacheHandler.syncOnDelete(id, subSystemCodes)
        }

        // 2. 再删除租户
        val success = super.deleteById(id)
        if (success) {
            // 同步缓存
            tenantByIdCacheHandler.syncOnDelete(id)
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

        // 2.删除租户-子系统关系
        val criteria = Criteria.of(SysTenantSubSystem::tenantId.name, OperatorEnum.IN, ids)
        val count = sysTenantSubSystemBiz.batchDeleteCriteria(criteria)

        // 3.删除租户
        if (count >= 0) {
            val count = super.batchDelete(ids)
            log.debug("批量删除租户，期望删除${ids.size}条，实际删除${count}条。")
        }

        // 3.同步缓存
        tenantByIdCacheHandler.syncOnBatchDelete(ids)
        tenantIdsBySubSysCacheHandler.syncOnBatchDelete(ids, subSystemCodes)
        return count
    }


    override fun getAllActiveTenants(): Map<String, List<SysTenantRecord>> {
        val searchPayload = SysTenantSearchPayload().apply { active = true }
        @Suppress("UNCHECKED_CAST")
        val records = dao.search(searchPayload) as List<SysTenantRecord>
        // 根据租户的子系统关系分组
        val tenantIds = records.mapNotNull { it.id }
        val tenantSubSystemMap = sysTenantSubSystemBiz.groupingSubSystemCodesByTenantIds(tenantIds)
        val result = mutableMapOf<String, MutableList<SysTenantRecord>>()
        records.forEach { tenant ->
            val subSystemCodes = tenantSubSystemMap[tenant.id] ?: emptyList()
            if (subSystemCodes.isEmpty()) {
                // 没有子系统的租户，使用空字符串作为key
                result.getOrPut("") { mutableListOf() }.add(tenant)
            } else {
                subSystemCodes.forEach { subSystemCode ->
                    result.getOrPut(subSystemCode) { mutableListOf() }.add(tenant)
                }
            }
        }
        return result
    }

    /**
     * 根据id获取租户记录（非缓存）
     *
     * @param id 主键
     * @return 租户记录，找不到返回null
     * @author AI: Cursor
     * @since 1.0.0
     */
    override fun getTenantRecord(id: String): SysTenantRecord? {
        val tenant = dao.get(id) ?: return null
        val record = SysTenantRecord()
        BeanKit.copyProperties(tenant, record)
        return record
    }

    /**
     * 根据名称获取租户记录
     *
     * @param name 租户名称
     * @return 租户记录，找不到返回null
     * @author AI: Cursor
     * @since 1.0.0
     */
    override fun getTenantByName(name: String): SysTenantRecord? {
        val searchPayload = SysTenantSearchPayload().apply {
            this.name = name
        }
        @Suppress("UNCHECKED_CAST")
        val records = dao.search(searchPayload) as List<SysTenantRecord>
        return records.firstOrNull()
    }

    /**
     * 获取租户的子系统编码列表
     *
     * @param tenantId 租户id
     * @return 子系统编码集合
     * @author AI: Cursor
     * @since 1.0.0
     */
    override fun getSubSystemCodesByTenantId(tenantId: String): Set<String> {
        return sysTenantSubSystemBiz.searchSubSystemCodesByTenantId(tenantId)
    }

    //endregion your codes 2

}