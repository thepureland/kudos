package io.kudos.ms.sys.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.PagingSearchResult
import io.kudos.base.query.eq
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.ms.sys.common.vo.tenant.SysTenantCacheItem
import io.kudos.ms.sys.common.vo.tenant.SysTenantDetail
import io.kudos.ms.sys.common.vo.tenant.SysTenantPayload
import io.kudos.ms.sys.common.vo.tenant.SysTenantRecord
import io.kudos.ms.sys.core.cache.SysTenantSystemHashCache
import io.kudos.ms.sys.core.cache.TenantByIdCache
import io.kudos.ms.sys.core.dao.SysTenantDao
import io.kudos.ms.sys.core.model.po.SysTenant
import io.kudos.ms.sys.core.model.po.SysTenantSystem
import io.kudos.ms.sys.core.service.iservice.ISysTenantService
import io.kudos.ms.sys.core.service.iservice.ISysTenantSystemService
import jakarta.annotation.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass


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

    @Resource
    private lateinit var tenantByIdCache: TenantByIdCache

    @Resource
    private lateinit var sysTenantSystemHashCache: SysTenantSystemHashCache

    @Resource
    private lateinit var sysTenantSystemService: ISysTenantSystemService

    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        val result = super.get(id, returnType)
        if (result is SysTenantDetail) {
            result.subSystemCodes = getSubSystemCodesString(id)
        } else if(result is SysTenantPayload) {
            result.subSystemCodes = sysTenantSystemHashCache.getSubSystemCodesByTenantId(id)
        }
        return result
    }

    override fun pagingSearch(listSearchPayload: ListSearchPayload): PagingSearchResult<*> {
        val result = super.pagingSearch(listSearchPayload)
        result.data.forEach {
            if (it is SysTenantRecord) {
                it.subSystemCodes = getSubSystemCodesString(it.id)
            }
        }
        return result
    }

    override fun getTenant(id: String): SysTenantCacheItem? {
        return tenantByIdCache.getTenantById(id)
    }

    override fun getTenantsBySubSystemCode(ids: Collection<String>): Map<String, SysTenantCacheItem> {
        return tenantByIdCache.getTenantsByIds(ids)
    }

    override fun getTenantsBySubSystemCode(subSystemCode: String): List<SysTenantCacheItem> {
        val tenantIds = sysTenantSystemHashCache.getTenantIdsBySubSystemCode(subSystemCode)
        return tenantByIdCache.getTenantsByIds(tenantIds).values.filter { it.active }
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的租户。")

        // 保存租户-系统关系
        if (any is SysTenantPayload) {
            insertSysTenantSystems(any)
        }

        // 同步缓存
        tenantByIdCache.syncOnInsert(id)
        sysTenantSystemHashCache.syncOnInsert(any, id)
        return id
    }

    private fun insertSysTenantSystems(any: SysTenantPayload) {
        val tenantSystems = any.subSystemCodes.mapTo(mutableSetOf()) { subSystemCode ->
            SysTenantSystem().apply {
                this.systemCode = subSystemCode
                this.tenantId = any.id.toString()
            }
        }
        sysTenantSystemService.batchInsert(tenantSystems)
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, SysTenant::id.name) as String
        if (success) {
            if (any is SysTenantPayload) {
                val tenantId = any.id!!
                val subSystemCodes = sysTenantSystemHashCache.getSubSystemCodesByTenantId(tenantId)
                // 判断租户-系统关系有没有变，有变要更新租户-系统关系
                if (subSystemCodes != any.subSystemCodes) {
                    // 先删除该租户的所有与系统的关系记录
                    sysTenantSystemService.deleteByTenantId(tenantId)

                    // 再插入新的关系
                    insertSysTenantSystems(any)
                }
            }

            // 同步缓存
            tenantByIdCache.syncOnUpdate(id)

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
            tenantByIdCache.syncOnUpdate(id)
        } else {
            log.error("更新id为${id}的租户的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        // 1. 先删除租户-系统关系
        val count = sysTenantSystemService.deleteByTenantId(id)
        if (count > 0) {
            // 同步缓存
            sysTenantSystemHashCache.syncOnDelete(id)
        } else {
            log.error("删除id为${id}的租户的所有系统关系失败！")
            return false
        }

        // 2. 再删除租户
        var success = super.deleteById(id)
        if (success) {
            // 同步缓存
            tenantByIdCache.syncOnDelete(id)
        } else {
            success = false
            log.error("删除id为${id}的租户失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        // 1.删除租户-子系统关系
        var count = sysTenantSystemService.batchDeleteByTenantIds(ids)

        // 2.删除租户
        if (count >= 0) {
            count = super.batchDelete(ids)
            log.debug("批量删除租户，期望删除${ids.size}条，实际删除${count}条。")

            // 同步缓存
            tenantByIdCache.syncOnBatchDelete(ids)
        }

        return count
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
        val criteria = Criteria(SysTenant::name eq name)
        val records = dao.searchAs<SysTenantRecord>(criteria)
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
        return sysTenantSystemService.searchSystemCodesByTenantId(tenantId)
    }

    private fun getSubSystemCodesString(tenantId: String): String {
        return sysTenantSystemHashCache.getSubSystemCodesByTenantId(tenantId).joinToString(", ")
    }

    //endregion your codes 2

}
