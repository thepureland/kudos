package io.kudos.ms.auth.core.role.cache
import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.common.role.vo.AuthRoleCacheEntry
import io.kudos.ms.auth.core.role.cache.AuthRoleHashCache.Companion.CACHE_NAME
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import jakarta.annotation.Resource
import org.springframework.stereotype.Component

/**
 * 角色 Hash 缓存处理器
 *
 * 数据来源表：auth_role；主键为 id，缓存的 key 即 id，value 为 [AuthRoleCacheEntry]。
 * 使用 Hash 结构存储，支持按 id 存取、按 tenantId+code 二级索引查询
 *
 * - 按 id：getRoleById、getRolesByIds
 * - 按租户+角色编码：getRoleByTenantIdAndRoleCode
 *
 * 使用前需在缓存配置表 sys_cache 中增加名为 [CACHE_NAME] 的配置项且 hash=true。
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class AuthRoleHashCache : AbstractHashCacheHandler<AuthRoleCacheEntry>() {

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    private val log = LogFactory.getLog(this::class)

    companion object {
        const val CACHE_NAME = "AUTH_ROLE__HASH"

        /** 可筛选副属性：按 tenantId、code 建二级索引（不包含 active） */
        val FILTERABLE_PROPERTIES = setOf(
            AuthRoleCacheEntry::tenantId.name,
            AuthRoleCacheEntry::code.name
        )
    }

    override fun cacheName(): String = CACHE_NAME

    override fun entityClass() = AuthRoleCacheEntry::class

    override fun filterableProperties(): Set<String> = FILTERABLE_PROPERTIES

    override fun doReload(id: Any): AuthRoleCacheEntry? = authRoleDao.getAs(id.toString())

    /**
     * 根据 id 从缓存获取角色，未命中则查库并回写。
     *
     * @param id 角色 id（主键），非空
     * @return 角色缓存项，不存在时 null
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#id",
        entityClass = AuthRoleCacheEntry::class,
        unless = "#result == null",
        filterableProperties = ["tenantId", "code"]
    )
    open fun getRoleById(id: String): AuthRoleCacheEntry? {
        require(id.isNotBlank()) { "获取角色时 id 不能为空" }
        return authRoleDao.getAs<AuthRoleCacheEntry>(id)
    }

    /**
     * 根据多个 id 从缓存批量获取角色，未命中的从库加载并回写。
     *
     * @param ids 角色 id 集合，可为空
     * @return id -> 实体 映射，仅包含能查到的 id
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = AuthRoleCacheEntry::class,
        filterableProperties = ["tenantId", "code"]
    )
    open fun getRolesByIds(ids: Collection<String>): Map<String, AuthRoleCacheEntry> {
        if (ids.isEmpty()) return emptyMap()
        val list = authRoleDao.getByIdsAs<AuthRoleCacheEntry>(ids)
        return list.filter { it.id.isNotBlank() && it.id in ids }.associateBy { it.id }
    }

    /**
     * 根据租户、角色编码从缓存获取角色，未命中则查库并回写（不区分 active）。
     *
     * @param tenantId 租户 id
     * @param code 角色编码
     * @return 角色缓存项，不存在时 null
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#tenantId", "#code"],
        entityClass = AuthRoleCacheEntry::class,
        filterableProperties = ["tenantId", "code"]
    )
    open fun getRoleByTenantIdAndRoleCode(tenantId: String, code: String): AuthRoleCacheEntry? {
        return authRoleDao.searchRoleByTenantIdAndRoleCode(tenantId, code)
    }

    /**
     * 从库全量加载角色并刷新 Hash 缓存（含 active=false，等价原 RoleByIdCache 全量）。
     *
     * @param clear 为 true 时先清空再写入；为 false 时覆盖写入
     */
    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载角色 Hash 缓存")
            return
        }
        val cache = hashCache()
        if (clear) cache.clear(CACHE_NAME)
        val list = authRoleDao.searchAs<AuthRoleCacheEntry>()
        log.debug("从数据库加载 ${list.size} 条角色，刷新 Hash 缓存")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
    }

    /** 新增角色后同步：将指定 id 的实体从库加载并写入缓存。 */
    open fun syncOnInsert(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME) || !KeyValueCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = authRoleDao.getAs<AuthRoleCacheEntry>(id) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /** 更新角色后同步：从库重新加载并写入缓存。 */
    open fun syncOnUpdate(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val item = authRoleDao.getAs<AuthRoleCacheEntry>(id) ?: return
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /** 删除角色后同步：从缓存中移除该 id。 */
    open fun syncOnDelete(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, AuthRoleCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /** 批量删除角色后同步：从缓存中移除这些 id。 */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val cache = hashCache()
        ids.forEach { cache.deleteById(CACHE_NAME, it, AuthRoleCacheEntry::class, FILTERABLE_PROPERTIES, emptySet()) }
    }
}
