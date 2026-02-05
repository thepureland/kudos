package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import io.kudos.ms.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.ms.sys.core.cache.SysResourceHashCache.Companion.CACHE_NAME
import io.kudos.ms.sys.core.cache.SysResourceHashCache.Companion.FILTERABLE_PROPERTIES
import io.kudos.ms.sys.core.dao.SysResourceDao
import jakarta.annotation.Resource
import org.springframework.stereotype.Component

/**
 * 系统资源统一缓存处理器，基于 Hash 结构存储 [SysResourceCacheItem]。
 *
 * 提供三类查询与回写能力：
 * - **按主键**：按 id 取单条或批量实体。
 * - **按子系统+URL**：按子系统编码、URL、启用状态取单条资源 id 或实体列表。
 * - **按子系统+资源类型**：按子系统编码、资源类型、启用状态取资源 id 列表或实体列表。
 *
 * 使用 [FILTERABLE_PROPERTIES] 中的副属性建立 Set 索引，支持多条件等值查询；所有写入、删除、全量刷新均需使用同一副属性集合以保持索引一致。
 *
 * 使用前需在缓存配置表sys_cache中增加名为 [CACHE_NAME] 的配置项。
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysResourceHashCache : AbstractHashCacheHandler<SysResourceCacheItem>() {

    @Resource
    private lateinit var sysResourceDao: SysResourceDao

    private val log = LogFactory.getLog(this)

    companion object {
        const val CACHE_NAME = "SYS_RESOURCE__HASH"

        /** 用于等值筛选与 Set 索引的副属性名集合，写入/删除/全量刷新时须与此一致（不含 active，不建 active 二级索引） */
        val FILTERABLE_PROPERTIES = setOf(
            SysResourceCacheItem::subSystemCode.name,
            SysResourceCacheItem::url.name,
            SysResourceCacheItem::resourceTypeDictCode.name
        )
    }

    override fun cacheName(): String = CACHE_NAME

    // ---------- 1. 按主键 id ----------

    /**
     * 根据主键 id 获取单条资源实体。
     * 先查缓存，未命中则查库并回写；回写时按 [FILTERABLE_PROPERTIES] 建立副属性索引。
     *
     * @param id 资源主键，非空
     * @return 资源缓存项，不存在时 null
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#id",
        entityClass = SysResourceCacheItem::class,
        unless = "#result == null",
        filterableProperties = ["subSystemCode", "url", "resourceTypeDictCode"]
    )
    open fun getResourceById(id: String): SysResourceCacheItem? {
        require(id.isNotBlank()) { "获取资源时 id 不能为空" }
        return sysResourceDao.getCacheItem(id)
    }

    /**
     * 根据主键 id 列表批量获取资源实体。
     * 先查缓存，未命中的 id 再查库并回写；回写时按 [FILTERABLE_PROPERTIES] 建立副属性索引。
     *
     * @param ids 资源主键列表，可为空
     * @return id -> 实体 映射，仅包含能查到的 id
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = SysResourceCacheItem::class,
        filterableProperties = ["subSystemCode", "url", "resourceTypeDictCode"]
    )
    open fun getResourcesByIds(ids: List<String>): Map<String, SysResourceCacheItem> {
        if (ids.isEmpty()) return emptyMap()
        val list = sysResourceDao.listCacheItemsByIds(ids)
        return ids.associateWith { id -> list.first { it.id == id } }
    }

    // ---------- 2. 按子系统+URL ----------

    /**
     * 按子系统编码、URL、启用状态多条件等值查询，返回匹配的资源实体列表（0 或 1 条）。
     * 先按副属性索引查缓存，未命中则查库并回写。
     *
     * @param subSystemCode 子系统编码，非空
     * @param url 资源 URL，非空
     * @return 匹配的实体列表
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#subSystemCode", "#url"],
        entityClass = SysResourceCacheItem::class,
        filterableProperties = ["subSystemCode", "url", "resourceTypeDictCode"]
    )
    open fun getResourceBySubSystemCodeAndUrl(subSystemCode: String, url: String): SysResourceCacheItem? {
        return sysResourceDao.getResourceBySubSysAndUrl(subSystemCode, url)
    }

    // ---------- 3. 按子系统+资源类型 ----------

    /**
     * 按子系统编码、资源类型、启用状态多条件等值查询，返回匹配的资源实体列表。
     * 先按副属性索引查缓存，未命中则查库并回写。
     *
     * @param subSystemCode 子系统编码，非空
     * @param resourceTypeDictCode 资源类型字典码，非空
     * @return 匹配的实体列表
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#subSystemCode", "#resourceTypeDictCode"],
        entityClass = SysResourceCacheItem::class,
        filterableProperties = ["subSystemCode", "url", "resourceTypeDictCode"]
    )
    open fun getResourcesBySubSystemCodeAndType(subSystemCode: String, resourceTypeDictCode: String): List<SysResourceCacheItem> {
        val ids = sysResourceDao.getResourceIdsBySubSysAndType(subSystemCode, resourceTypeDictCode)
        if (ids.isEmpty()) return emptyList()
        return sysResourceDao.listCacheItemsByIds(ids)
    }

    // ---------- 全量刷新 ----------

    /**
     * 从库全量加载资源并刷新 Hash 缓存。
     *
     * @param clear 为 true 时先清空当前缓存再写入；为 false 时覆盖写入
     */
    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载资源 Hash 缓存")
            return
        }
        val cache = hashCache()
        val list = sysResourceDao.listAllCacheItems()
        log.debug("从数据库加载 ${list.size} 条资源，刷新 Hash 缓存")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
        log.debug("资源 Hash 缓存刷新完成")
    }

    // ---------- 写库后同步（供业务在新增/更新/删除后调用） ----------

    /**
     * 新增资源后同步：将指定 id 的实体从库加载并写入缓存，并建立副属性索引。
     *
     * @param id 新增的资源主键
     */
    open fun syncOnInsert(id: String) {
        if (!CacheKit.isCacheActive(CACHE_NAME) || !CacheKit.isWriteInTime(CACHE_NAME)) return
        val item = sysResourceDao.getCacheItem(id) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * 新增资源后同步（重载，接收业务对象与 id）。行为同 [syncOnInsert(id)]。
     *
     * @param any 业务对象，仅用于重载区分
     * @param id 新增的资源主键
     */
    open fun syncOnInsert(any: Any, id: String) {
        syncOnInsert(id)
    }

    /**
     * 更新资源后同步：将指定 id 的实体从库重新加载并写入缓存，更新副属性索引。
     *
     * @param id 被更新的资源主键
     */
    open fun syncOnUpdate(id: String) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) return
        val item = sysResourceDao.getCacheItem(id) ?: return
        if (CacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /**
     * 更新资源后同步（重载，带旧 URL 等参数）。行为同 [syncOnUpdate(id)]，Hash 结构下直接覆盖即可。
     */
    open fun syncOnUpdate(any: Any, id: String, oldUrl: String?) {
        syncOnUpdate(id)
    }

    /**
     * 更新资源后同步（重载，带旧子系统、资源类型等参数）。行为同 [syncOnUpdate(id)]。
     */
    open fun syncOnUpdate(any: Any, id: String, oldSubSystemCode: String, oldResourceTypeDictCode: String) {
        syncOnUpdate(id)
    }

    /**
     * 更新资源启用状态后同步。行为同 [syncOnUpdate(id)]。
     *
     * @param id 资源主键
     * @param active 新的启用状态
     */
    open fun syncOnUpdateActive(id: String, active: Boolean) {
        syncOnUpdate(id)
    }

    /**
     * 更新资源启用状态后同步（重载，无 active 参数）。行为同 [syncOnUpdate(id)]。
     */
    open fun syncOnUpdateActive(id: String) {
        syncOnUpdate(id)
    }

    /**
     * 删除资源后同步：从缓存中移除该 id，并从副属性 Set 索引中移除。
     *
     * @param id 被删除的资源主键
     * @param subSystemCode 该资源所属子系统编码（用于索引移除）
     * @param urlOrResourceType URL 或资源类型，仅用于索引移除，可为 null
     */
    open fun syncOnDelete(id: String, subSystemCode: String, urlOrResourceType: String?) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, SysResourceCacheItem::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * 批量删除资源后同步：从缓存中移除这些 id，并从副属性 Set 索引中移除。
     *
     * @param ids 被删除的资源主键集合
     */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) return
        val cache = hashCache()
        ids.forEach { cache.deleteById(CACHE_NAME, it, SysResourceCacheItem::class, FILTERABLE_PROPERTIES, emptySet()) }
    }

    /**
     * 生成「子系统+URL」维度的组合 key，格式：子系统编码 + 分隔符 + URL。
     * 用于外部需要与缓存 key 约定一致的场景。
     */
    fun getKeySubSysAndUrl(subSystemCode: String, url: String?): String {
        return "${subSystemCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${url}"
    }

    /**
     * 生成「子系统+资源类型」维度的组合 key，格式：子系统编码 + 分隔符 + 资源类型字典码。
     * 用于外部需要与缓存 key 约定一致的场景。
     */
    fun getKeySubSysAndType(subSystemCode: String, resourceTypeDictCode: String): String {
        return "${subSystemCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${resourceTypeDictCode}"
    }
}
