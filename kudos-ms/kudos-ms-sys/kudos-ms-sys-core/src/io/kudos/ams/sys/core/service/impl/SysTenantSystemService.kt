package io.kudos.ms.sys.core.service.impl

import io.kudos.ms.sys.core.service.iservice.ISysTenantSystemService
import io.kudos.ms.sys.core.model.po.SysTenantSystem
import io.kudos.ms.sys.core.dao.SysTenantSystemDao
import io.kudos.ms.sys.core.cache.TenantIdsBySystemCodeCacheHandler
import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.logger.LogFactory
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 租户-系统关系业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysTenantSystemService : BaseCrudService<String, SysTenantSystem, SysTenantSystemDao>(), ISysTenantSystemService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    @Autowired
    private lateinit var tenantIdsBySystemCodeCacheHandler: TenantIdsBySystemCodeCacheHandler

    override fun searchSystemCodesByTenantId(tenantId: String): Set<String> =
        dao.searchSystemCodesByTenantId(tenantId)

    override fun searchTenantIdsBySystemCode(systemCode: String): Set<String> =
        dao.searchTenantIdsBySystemCode(systemCode)

    override fun groupingSystemCodesByTenantIds(tenantIds: Collection<String>?): Map<String, List<String>> =
        dao.groupingSystemCodesByTenantIds(tenantIds)

    override fun groupingTenantIdsBySystemCodes(systemCodes: Collection<String>?): Map<String, List<String>> =
        dao.groupingTenantIdsBySystemCodes(systemCodes)

    /**
     * 批量绑定租户与系统的关系
     *
     * @param tenantId 租户id
     * @param systemCodes 系统编码集合
     * @return 成功绑定的数量
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional
    override fun batchBind(tenantId: String, systemCodes: Collection<String>): Int {
        if (systemCodes.isEmpty()) {
            return 0
        }
        var count = 0
        val insertedSystemCodes = mutableSetOf<String>()
        systemCodes.forEach { systemCode ->
            if (!exists(tenantId, systemCode)) {
                val relation = SysTenantSystem {
                    this.tenantId = tenantId
                    this.systemCode = systemCode
                }
                dao.insert(relation)
                insertedSystemCodes.add(systemCode)
                count++
            }
        }
        log.debug("批量绑定租户${tenantId}与${systemCodes.size}个系统的关系，成功绑定${count}条。")
        // 同步缓存
        insertedSystemCodes.forEach { systemCode ->
            tenantIdsBySystemCodeCacheHandler.evict(systemCode)
            if (CacheKit.isWriteInTime(tenantIdsBySystemCodeCacheHandler.cacheName())) {
                tenantIdsBySystemCodeCacheHandler.getTenantIds(systemCode)
            }
        }
        return count
    }

    /**
     * 解绑租户与系统的关系
     *
     * @param tenantId 租户id
     * @param systemCode 系统编码
     * @return 是否解绑成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional
    override fun unbind(tenantId: String, systemCode: String): Boolean {
        val criteria = Criteria.of(SysTenantSystem::tenantId.name, OperatorEnum.EQ, tenantId)
            .addAnd(SysTenantSystem::systemCode.name, OperatorEnum.EQ, systemCode)
        val count = dao.batchDeleteCriteria(criteria)
        val success = count > 0
        if (success) {
            log.debug("解绑租户${tenantId}与系统${systemCode}的关系。")
            // 同步缓存
            tenantIdsBySystemCodeCacheHandler.syncOnDelete(tenantId, setOf(systemCode))
        } else {
            log.warn("解绑租户${tenantId}与系统${systemCode}的关系失败，关系不存在。")
        }
        return success
    }

    /**
     * 检查关系是否存在
     *
     * @param tenantId 租户id
     * @param systemCode 系统编码
     * @return 是否存在
     * @author AI: Cursor
     * @since 1.0.0
     */
    override fun exists(tenantId: String, systemCode: String): Boolean {
        return dao.exists(tenantId, systemCode)
    }

    /**
     * 新增租户-系统关系
     *
     * @param any 关系对象
     * @return 主键
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的租户-系统关系。")
        // 同步缓存
        tenantIdsBySystemCodeCacheHandler.syncOnInsert(any, id)
        return id
    }

    /**
     * 删除租户-系统关系
     *
     * @param id 主键
     * @return 是否删除成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional
    override fun deleteById(id: String): Boolean {
        val relation = dao.get(id)
        if (relation == null) {
            log.warn("删除id为${id}的租户-系统关系时，发现其已不存在！")
            return false
        }
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除id为${id}的租户-系统关系。")
            // 同步缓存
            tenantIdsBySystemCodeCacheHandler.syncOnDelete(relation.tenantId, setOf(relation.systemCode))
        } else {
            log.error("删除id为${id}的租户-系统关系失败！")
        }
        return success
    }

    /**
     * 批量删除租户-系统关系
     *
     * @param ids 主键集合
     * @return 删除的数量
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        @Suppress("UNCHECKED_CAST")
        val relations = dao.inSearchById(ids)
        val tenantSubSystemMap = relations.groupBy { it.tenantId }
        val count = super.batchDelete(ids)
        log.debug("批量删除租户-系统关系，期望删除${ids.size}条，实际删除${count}条。")
        // 同步缓存
        tenantSubSystemMap.forEach { (tenantId, reals) ->
            val systemCodes = reals.map { it.systemCode }.toSet()
            tenantIdsBySystemCodeCacheHandler.syncOnDelete(tenantId, systemCodes)
        }
        return count
    }

    //endregion your codes 2

}
