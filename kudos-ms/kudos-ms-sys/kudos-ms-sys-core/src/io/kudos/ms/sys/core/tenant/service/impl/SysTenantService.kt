package io.kudos.ms.sys.core.tenant.service.impl

import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.model.vo.IdAndName
import io.kudos.base.query.Criteria
import io.kudos.base.query.PagingSearchResult
import io.kudos.base.query.eq
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.tenant.vo.SysTenantCacheEntry
import io.kudos.ms.sys.common.tenant.vo.request.SysTenantFormCreate
import io.kudos.ms.sys.common.tenant.vo.request.SysTenantFormUpdate
import io.kudos.ms.sys.common.tenant.vo.response.SysTenantDetail
import io.kudos.ms.sys.common.tenant.vo.response.SysTenantRow
import io.kudos.ms.sys.core.tenant.cache.SysTenantSystemHashCache
import io.kudos.ms.sys.core.tenant.cache.TenantByIdCache
import io.kudos.ms.sys.core.tenant.dao.SysTenantDao
import io.kudos.ms.sys.core.tenant.event.SysTenantBatchDeleted
import io.kudos.ms.sys.core.tenant.event.SysTenantDeleted
import io.kudos.ms.sys.core.tenant.event.SysTenantInserted
import io.kudos.ms.sys.core.tenant.event.SysTenantUpdated
import io.kudos.ms.sys.core.tenant.model.po.SysTenant
import io.kudos.ms.sys.core.tenant.model.po.SysTenantSystem
import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantService
import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantSystemService
import org.springframework.context.ApplicationEventPublisher
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
@Transactional
open class SysTenantService(
    dao: SysTenantDao,
    private val tenantByIdCache: TenantByIdCache,
    private val sysTenantSystemHashCache: SysTenantSystemHashCache,
    private val sysTenantSystemService: ISysTenantSystemService,
    private val eventPublisher: ApplicationEventPublisher,
) : BaseCrudService<String, SysTenant, SysTenantDao>(dao), ISysTenantService {

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun <R : Any> get(id: String, returnType: KClass<R>): R? =
        if (returnType == SysTenantCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            tenantByIdCache.getTenantById(id) as R?
        } else {
            enrichTenantDetail(super.get(id, returnType), id)
        }

    @Transactional(readOnly = true)
    override fun pagingSearch(listSearchPayload: ListSearchPayload): PagingSearchResult<*> {
        val result = super.pagingSearch(listSearchPayload)
        result.data.filterIsInstance<SysTenantRow>().forEach(::enrichTenantRow)
        return result
    }

    @Transactional(readOnly = true)
    override fun getTenantFromCache(id: String): SysTenantCacheEntry? = tenantByIdCache.getTenantById(id)

    @Transactional(readOnly = true)
    override fun getTenantsFromCacheByIds(ids: Collection<String>): Map<String, SysTenantCacheEntry> =
        ids.takeIf { it.isNotEmpty() }?.let(tenantByIdCache::getTenantsByIds) ?: emptyMap()

    @Transactional(readOnly = true)
    override fun getTenantsForSubSystemFromCache(subSystemCode: String): List<SysTenantCacheEntry> =
        tenantByIdCache.getTenantsByIds(sysTenantSystemHashCache.getTenantIdsBySubSystemCode(subSystemCode)).values.toList()

    @Transactional(readOnly = true)
    override fun getActiveTenantIdAndNamesForSubSystem(subSystemCode: String): List<IdAndName<String>> =
        getTenantsForSubSystemFromCache(subSystemCode)
            .filter { it.active }
            .map { IdAndName(it.id, it.name) }

    @Transactional(readOnly = true)
    override fun getAllTenantsFromCache(): List<SysTenantCacheEntry> = dao.searchAs()

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "新增id为${id}的租户。") {
            // insertTenantSystemsOnCreate 经 sysTenantSystemService.batchInsert 创建关系；
            // SysTenantSystemHashCache 由其按需 lazy load 触发回填，无需此处主动 sync。
            insertTenantSystemsOnCreate(any, id)
            eventPublisher.publishEvent(SysTenantInserted(id = id))
        }
        return id
    }

    /**
     * 批量插入"租户 ↔ 子系统"关联表行。
     *
     * 把 `subSystemCodes` 每个 code 映射成 [SysTenantSystem] 实体后走 `batchInsert`，
     * 一次写入避免 N+1。
     *
     * @param tenantId 租户 id
     * @param subSystemCodes 子系统编码集合
     * @author K
     * @since 1.0.0
     */
    private fun insertSysTenantSystems(tenantId: String, subSystemCodes: Set<String>) {
        val tenantSystems = subSystemCodes.mapTo(mutableSetOf()) { subSystemCode ->
            SysTenantSystem().apply {
                this.systemCode = subSystemCode
                this.tenantId = tenantId
            }
        }
        sysTenantSystemService.batchInsert(tenantSystems)
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireTenantId(any)
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "更新id为${id}的租户。",
            failureMessage = "更新id为${id}的租户失败！",
        ) {
            syncTenantSystemsOnUpdate(any)
            eventPublisher.publishEvent(SysTenantUpdated(id = id))
        }
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val tenant = SysTenant {
            this.id = id
            this.active = active
        }
        return completeCrudUpdate(
            success = dao.update(tenant),
            log = log,
            successMessage = "更新id为${id}的租户的启用状态为${active}。",
            failureMessage = "更新id为${id}的租户的启用状态为${active}失败！",
        ) {
            eventPublisher.publishEvent(SysTenantUpdated(id = id))
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        if (dao.get(id) == null) {
            log.warn("删除id为${id}的租户时，发现其已不存在！")
            return false
        }

        deleteTenantSystems(id)

        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "删除id为${id}的租户成功！",
            failureMessage = "删除id为${id}的租户失败！",
        ) {
            eventPublisher.publishEvent(SysTenantDeleted(id = id))
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        if (sysTenantSystemService.batchDeleteByTenantIds(ids) < 0) return 0
        val count = super.batchDelete(ids)
        log.debug("批量删除租户，期望删除${ids.size}条，实际删除${count}条。")
        if (count > 0) {
            eventPublisher.publishEvent(SysTenantBatchDeleted(ids = ids))
        }
        return count
    }

    @Transactional(readOnly = true)
    override fun getTenantRecord(id: String): SysTenantRow? = dao.get(id)?.let(::toSysTenantRow)

    @Transactional(readOnly = true)
    override fun getTenantByName(name: String): SysTenantRow? =
        dao.search(Criteria(SysTenant::name eq name)).firstOrNull()?.let(::toSysTenantRow)

    /**
     * 租户更新时同步"租户↔子系统"绑定：仅 [SysTenantFormUpdate] 入参才会触发，
     * 与缓存差异比对后才走 replace——避免无差异时空转 DB 和触发缓存失效抖动。
     *
     * @param any 更新入参，非 [SysTenantFormUpdate] 直接 no-op
     * @author K
     * @since 1.0.0
     */
    private fun syncTenantSystemsOnUpdate(any: Any) {
        if (any !is SysTenantFormUpdate) return

        val tenantId = requireNotNull(any.id) { "更新租户时 id 不能为空" }
        val subSystemCodes = sysTenantSystemHashCache.getSubSystemCodesByTenantId(tenantId)
        if (subSystemCodes != any.subSystemCodes) {
            replaceTenantSystems(tenantId, any.subSystemCodes)
        }
    }

    /**
     * "全量替换"语义：先清旧关联再插入新集合。
     * 简单粗暴但避免了"增量 diff"的复杂度——绑定数量很小，性能可接受。
     *
     * @param tenantId 租户 id
     * @param subSystemCodes 新的子系统集合
     * @author K
     * @since 1.0.0
     */
    private fun replaceTenantSystems(tenantId: String, subSystemCodes: Set<String>) {
        deleteTenantSystems(tenantId)
        insertSysTenantSystems(tenantId, subSystemCodes)
    }

    /**
     * 清除某租户名下所有子系统绑定。
     * 缓存失效由 `sysTenantSystemService.deleteByTenantId` 内部发的
     * `SysTenantSystemTenantsChanged` 事件驱动 `SysTenantSystemHashCache` 自动完成。
     *
     * @param tenantId 租户 id
     * @author K
     * @since 1.0.0
     */
    private fun deleteTenantSystems(tenantId: String) {
        // sysTenantSystemService.deleteByTenantId 已发布 SysTenantSystemTenantsChanged，
        // 由 SysTenantSystemHashCache.on(...) 订阅完成失效。
        sysTenantSystemService.deleteByTenantId(tenantId)
    }

    /**
     * 租户新建时按入参类型选择性插入子系统绑定：仅 [SysTenantFormCreate] 入参触发。
     *
     * @param any 新建入参
     * @param tenantId 已生成的租户 id
     * @author K
     * @since 1.0.0
     */
    private fun insertTenantSystemsOnCreate(any: Any, tenantId: String) {
        if (any is SysTenantFormCreate) {
            insertSysTenantSystems(tenantId, any.subSystemCodes)
        }
    }

    /**
     * 详情对象的增强：仅 [SysTenantDetail] 类型才填充 `subSystemCodes` 字段，其他返回类型原样返回。
     *
     * @param R 返回类型
     * @param result 待增强对象
     * @param tenantId 租户 id
     * @return 增强后的对象（可能未被修改）
     * @author K
     * @since 1.0.0
     */
    private fun <R : Any> enrichTenantDetail(result: R?, tenantId: String): R? {
        if (result is SysTenantDetail) {
            result.subSystemCodes = getSubSystemCodesString(tenantId)
        }
        return result
    }

    /**
     * 列表行的增强：直接给 [SysTenantRow] 设 `subSystemCodes` 字符串字段（逗号分隔）。
     *
     * @param row 待增强的列表行
     * @author K
     * @since 1.0.0
     */
    private fun enrichTenantRow(row: SysTenantRow) {
        row.subSystemCodes = getSubSystemCodesString(row.id)
    }

    /**
     * 把 PO [SysTenant] 拷成扁平 VO [SysTenantRow]，用于 list 接口。
     *
     * @param tenant 租户 PO
     * @return 租户 VO（`subSystemCodes` 字段未填，由 [enrichTenantRow] 后续补齐）
     * @author K
     * @since 1.0.0
     */
    private fun toSysTenantRow(tenant: SysTenant): SysTenantRow = SysTenantRow(
        id = tenant.id,
        name = tenant.name,
        timezone = tenant.timezone,
        defaultLanguageCode = tenant.defaultLanguageCode,
        createTime = tenant.createTime,
        remark = tenant.remark,
        active = tenant.active,
        builtIn = tenant.builtIn,
    )

    @Transactional(readOnly = true)
    override fun getSubSystemCodesFromCache(tenantId: String): Set<String> =
        sysTenantSystemHashCache.getSubSystemCodesByTenantId(tenantId)

    /**
     * 把租户绑定的子系统集合拍扁成逗号分隔字符串，用于在列表/详情里直接展示。
     *
     * @param tenantId 租户 id
     * @return 形如 `"sys, msg, user"` 的字符串；无绑定时为空串
     * @author K
     * @since 1.0.0
     */
    private fun getSubSystemCodesString(tenantId: String): String =
        sysTenantSystemHashCache.getSubSystemCodesByTenantId(tenantId).joinToString(", ")

    /**
     * 从 update 入参抽 id；要求实现 [IIdEntity] 且 id 是 String。不满足直接 [error]。
     *
     * @param any 更新入参
     * @return 租户 id
     * @throws IllegalStateException 入参类型不被支持
     * @author K
     * @since 1.0.0
     */
    private fun requireTenantId(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("更新租户时不支持的入参类型: ${any::class.qualifiedName}")
}
