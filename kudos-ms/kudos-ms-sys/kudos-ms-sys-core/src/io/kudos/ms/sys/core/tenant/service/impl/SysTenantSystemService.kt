package io.kudos.ms.sys.core.tenant.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.core.tenant.cache.SysTenantSystemHashCache
import io.kudos.ms.sys.core.tenant.dao.SysTenantSystemDao
import io.kudos.ms.sys.core.tenant.event.SysTenantSystemBound
import io.kudos.ms.sys.core.tenant.event.SysTenantSystemSystemsChanged
import io.kudos.ms.sys.core.tenant.event.SysTenantSystemTenantsChanged
import io.kudos.ms.sys.core.tenant.model.po.SysTenantSystem
import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantSystemService
import org.springframework.context.ApplicationEventPublisher
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
@Transactional
open class SysTenantSystemService(
    dao: SysTenantSystemDao,
    private val sysTenantSystemHashCache: SysTenantSystemHashCache,
    private val eventPublisher: ApplicationEventPublisher,
) : BaseCrudService<String, SysTenantSystem, SysTenantSystemDao>(dao), ISysTenantSystemService {

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun searchSystemCodesByTenantId(tenantId: String): Set<String> =
        dao.searchSystemCodesByTenantId(tenantId)

    @Transactional(readOnly = true)
    override fun searchTenantIdsBySystemCode(systemCode: String): Set<String> =
        sysTenantSystemHashCache.getTenantIdsBySubSystemCode(systemCode).toSet()

    @Transactional(readOnly = true)
    override fun groupingSystemCodesByTenantIds(tenantIds: Collection<String>?): Map<String, List<String>> =
        dao.groupingSystemCodesByTenantIds(tenantIds)

    @Transactional(readOnly = true)
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
        if (systemCodes.isEmpty()) return 0

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
        if (insertedSystemCodes.isNotEmpty()) {
            // 影响这些 system 维度的缓存；tenant 维度的下次按需 reload
            eventPublisher.publishEvent(SysTenantSystemSystemsChanged(systemCodes = insertedSystemCodes))
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
        val count = dao.deleteByTenantIdAndSystemCode(tenantId, systemCode)
        val success = count > 0
        if (success) {
            log.debug("解绑租户${tenantId}与系统${systemCode}的关系。")
            eventPublisher.publishEvent(SysTenantSystemTenantsChanged(tenantIds = listOf(tenantId)))
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
    @Transactional(readOnly = true)
    override fun exists(tenantId: String, systemCode: String): Boolean = dao.exists(tenantId, systemCode)

    @Transactional
    override fun deleteByTenantId(tenantId: String): Int {
        val systemCodes = searchSystemCodesByTenantId(tenantId)
        val count = dao.batchDeleteByTenantIds(listOf(tenantId))
        if (count > 0 && systemCodes.isNotEmpty()) {
            eventPublisher.publishEvent(SysTenantSystemTenantsChanged(tenantIds = listOf(tenantId)))
        }
        return count
    }

    @Transactional
    override fun batchDeleteByTenantIds(tenantIds: Collection<String>): Int {
        if (tenantIds.isEmpty()) return 0
        val tenantAndSystemCodes = groupingSystemCodesByTenantIds(tenantIds)
        val systemCodes = tenantAndSystemCodes.values.flatten().toSet()
        val count = dao.batchDeleteByTenantIds(tenantIds)
        if (count > 0 && systemCodes.isNotEmpty()) {
            eventPublisher.publishEvent(SysTenantSystemTenantsChanged(tenantIds = tenantIds))
        }
        return count
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
        val relation = dao.get(id)
        if (relation != null) {
            eventPublisher.publishEvent(
                SysTenantSystemBound(id = id, tenantId = relation.tenantId, systemCode = relation.systemCode)
            )
        }
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
            eventPublisher.publishEvent(SysTenantSystemTenantsChanged(tenantIds = listOf(relation.tenantId)))
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
        val affectedTenantIds = relations.map { it.tenantId }.toSet()
        val count = super.batchDelete(ids)
        log.debug("批量删除租户-系统关系，期望删除${ids.size}条，实际删除${count}条。")
        if (count > 0 && affectedTenantIds.isNotEmpty()) {
            eventPublisher.publishEvent(SysTenantSystemTenantsChanged(tenantIds = affectedTenantIds))
        }
        return count
    }
}
