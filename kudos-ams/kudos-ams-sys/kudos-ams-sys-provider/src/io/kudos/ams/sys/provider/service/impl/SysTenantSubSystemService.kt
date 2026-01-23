package io.kudos.ams.sys.provider.service.impl

import io.kudos.ams.sys.provider.service.iservice.ISysTenantSubSystemService
import io.kudos.ams.sys.provider.model.po.SysTenantSubSystem
import io.kudos.ams.sys.provider.dao.SysTenantSubSystemDao
import io.kudos.ams.sys.provider.cache.TenantIdsBySubSysCacheHandler
import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.logger.LogFactory
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 租户-子系统关系业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysTenantSubSystemService : BaseCrudService<String, SysTenantSubSystem, SysTenantSubSystemDao>(), ISysTenantSubSystemService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    @Autowired
    private lateinit var tenantIdsBySubSysCacheHandler: TenantIdsBySubSysCacheHandler

    override fun searchSubSystemCodesByTenantId(tenantId: String): Set<String> =
        dao.searchSubSystemCodesByTenantId(tenantId)

    override fun searchTenantIdsBySubSystemCode(subSystemCode: String): Set<String> =
        dao.searchTenantIdsBySubSystemCode(subSystemCode)

    override fun groupingSubSystemCodesByTenantIds(tenantIds: Collection<String>?): Map<String, List<String>> =
        dao.groupingSubSystemCodesByTenantIds(tenantIds)

    override fun groupingTenantIdsBySubSystemCodes(subSystemCodes: Collection<String>?): Map<String, List<String>> =
        dao.groupingTenantIdsBySubSystemCodes(subSystemCodes)

    /**
     * 批量绑定租户与子系统的关系
     *
     * @param tenantId 租户id
     * @param subSystemCodes 子系统编码集合
     * @param portalCode 门户编码
     * @return 成功绑定的数量
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional
    override fun batchBind(tenantId: String, subSystemCodes: Collection<String>, portalCode: String): Int {
        if (subSystemCodes.isEmpty()) {
            return 0
        }
        var count = 0
        val insertedSubSystemCodes = mutableSetOf<String>()
        subSystemCodes.forEach { subSystemCode ->
            if (!exists(tenantId, subSystemCode)) {
                val relation = SysTenantSubSystem {
                    this.tenantId = tenantId
                    this.subSystemCode = subSystemCode
                    this.portalCode = portalCode
                }
                dao.insert(relation)
                insertedSubSystemCodes.add(subSystemCode)
                count++
            }
        }
        log.debug("批量绑定租户${tenantId}与${subSystemCodes.size}个子系统的关系，成功绑定${count}条。")
        // 同步缓存
        insertedSubSystemCodes.forEach { subSystemCode ->
            tenantIdsBySubSysCacheHandler.evict(subSystemCode)
            if (CacheKit.isWriteInTime(tenantIdsBySubSysCacheHandler.cacheName())) {
                tenantIdsBySubSysCacheHandler.getTenantIds(subSystemCode)
            }
        }
        return count
    }

    /**
     * 解绑租户与子系统的关系
     *
     * @param tenantId 租户id
     * @param subSystemCode 子系统编码
     * @return 是否解绑成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional
    override fun unbind(tenantId: String, subSystemCode: String): Boolean {
        val criteria = Criteria.of(SysTenantSubSystem::tenantId.name, OperatorEnum.EQ, tenantId)
            .addAnd(SysTenantSubSystem::subSystemCode.name, OperatorEnum.EQ, subSystemCode)
        val count = dao.batchDeleteCriteria(criteria)
        val success = count > 0
        if (success) {
            log.debug("解绑租户${tenantId}与子系统${subSystemCode}的关系。")
            // 同步缓存
            tenantIdsBySubSysCacheHandler.syncOnDelete(tenantId, setOf(subSystemCode))
        } else {
            log.warn("解绑租户${tenantId}与子系统${subSystemCode}的关系失败，关系不存在。")
        }
        return success
    }

    /**
     * 检查关系是否存在
     *
     * @param tenantId 租户id
     * @param subSystemCode 子系统编码
     * @return 是否存在
     * @author AI: Cursor
     * @since 1.0.0
     */
    override fun exists(tenantId: String, subSystemCode: String): Boolean {
        return dao.exists(tenantId, subSystemCode)
    }

    /**
     * 新增租户-子系统关系
     *
     * @param any 关系对象
     * @return 主键
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的租户-子系统关系。")
        // 同步缓存
        tenantIdsBySubSysCacheHandler.syncOnInsert(any, id)
        return id
    }

    /**
     * 删除租户-子系统关系
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
            log.warn("删除id为${id}的租户-子系统关系时，发现其已不存在！")
            return false
        }
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除id为${id}的租户-子系统关系。")
            // 同步缓存
            tenantIdsBySubSysCacheHandler.syncOnDelete(relation.tenantId, setOf(relation.subSystemCode))
        } else {
            log.error("删除id为${id}的租户-子系统关系失败！")
        }
        return success
    }

    /**
     * 批量删除租户-子系统关系
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
        log.debug("批量删除租户-子系统关系，期望删除${ids.size}条，实际删除${count}条。")
        // 同步缓存
        tenantSubSystemMap.forEach { (tenantId, rels) ->
            val subSystemCodes = rels.map { it.subSystemCode }.toSet()
            tenantIdsBySubSysCacheHandler.syncOnDelete(tenantId, subSystemCodes)
        }
        return count
    }

    //endregion your codes 2

}