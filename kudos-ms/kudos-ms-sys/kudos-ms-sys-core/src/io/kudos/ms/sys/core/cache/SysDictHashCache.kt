package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import io.kudos.ms.sys.common.vo.dict.SysDictCacheEntry
import io.kudos.ms.sys.core.cache.SysDictHashCache.Companion.CACHE_NAME
import io.kudos.ms.sys.core.cache.SysDictHashCache.Companion.FILTERABLE_PROPERTIES
import io.kudos.ms.sys.core.dao.SysDictDao
import jakarta.annotation.Resource
import org.springframework.stereotype.Component

/**
 * 字典类型 Hash 缓存处理器，基于 Hash 结构存储 [SysDictCacheEntry]。
 *
 * 数据来源表：sys_dict。
 *
 * 提供三类查询与回写能力：
 * - **按主键**：按 id 取单条或批量实体。
 * - **按原子服务编码+字典类型**：按 atomicServiceCode、dictType取单条字典实体。
 *
 * 使用 [FILTERABLE_PROPERTIES] 中的副属性建立 Set 索引，支持多条件等值查询；所有写入、删除、全量刷新均需使用同一副属性集合以保持索引一致。
 *
 * 使用前需在缓存配置表 sys_cache 中增加名为 [CACHE_NAME] 的配置项（hash=true）。
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysDictHashCache : AbstractHashCacheHandler<SysDictCacheEntry>() {

    @Resource
    private lateinit var sysDictDao: SysDictDao

    private val log = LogFactory.getLog(this::class)

    companion object {
        const val CACHE_NAME = "SYS_DICT__HASH"

        /** 用于等值筛选与 Set 索引的副属性名集合，写入/删除/全量刷新时须与此一致 */
        val FILTERABLE_PROPERTIES = setOf(
            SysDictCacheEntry::atomicServiceCode.name,
            SysDictCacheEntry::dictType.name
        )
    }

    override fun cacheName(): String = CACHE_NAME

    override fun entityClass() = SysDictCacheEntry::class

    override fun filterableProperties(): Set<String> = FILTERABLE_PROPERTIES

    override fun doReload(id: Any): SysDictCacheEntry? = sysDictDao.getAs(id.toString())

    // ---------- 1. 按主键 id ----------

    /**
     * 根据主键 id 获取单条字典实体。
     * 先查缓存，未命中则查库并回写；回写时按 [FILTERABLE_PROPERTIES] 建立副属性索引。
     *
     * @param id 字典主键，非空
     * @return 字典缓存项，不存在时 null
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#id",
        entityClass = SysDictCacheEntry::class,
        unless = "#result == null",
        filterableProperties = ["atomicServiceCode", "dictType"]
    )
    open fun getDictById(id: String): SysDictCacheEntry? {
        require(id.isNotBlank()) { "获取字典时 id 不能为空" }
        return sysDictDao.getAs<SysDictCacheEntry>(id)
    }

    /**
     * 根据主键 id 列表批量获取字典实体。
     * 先查缓存，未命中的 id 再查库并回写；回写时按 [FILTERABLE_PROPERTIES] 建立副属性索引。
     *
     * @param ids 字典主键列表，可为空
     * @return id -> 实体 映射，仅包含能查到的 id
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = SysDictCacheEntry::class,
        filterableProperties = ["atomicServiceCode", "dictType"]
    )
    open fun getDictsByIds(ids: Set<String>): Map<String, SysDictCacheEntry> {
        if (ids.isEmpty()) return emptyMap()
        val list = sysDictDao.getByIdsAs<SysDictCacheEntry>(ids)
        val byId = list.associateBy { it.id }
        return ids.mapNotNull { id -> byId[id]?.let { id to it } }.toMap()
    }

    // ---------- 2. 按原子服务编码 ----------

    /**
     * 按原子服务编码查询，返回匹配的字典实体列表。
     * 先按副属性索引查缓存，未命中则查库并回写。
     *
     * @param atomicServiceCode 原子服务编码，非空
     * @return 匹配的字典实体列表
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#atomicServiceCode"],
        entityClass = SysDictCacheEntry::class,
        filterableProperties = ["atomicServiceCode", "dictType"]
    )
    open fun getDictsByAtomicServiceCode(atomicServiceCode: String): List<SysDictCacheEntry> {
        return sysDictDao.searchDictsByAtomicServiceCode(atomicServiceCode)
    }

    // ---------- 3. 按原子服务编码+字典类型 ----------

    /**
     * 按原子服务编码、字典类型多条件等值查询，返回匹配的字典实体（0 或 1 条）。
     * 先按副属性索引查缓存，未命中则查库并回写。
     *
     * @param atomicServiceCode 原子服务编码，非空
     * @param dictType 字典类型，非空
     * @return 匹配的字典实体，不存在时 null
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#atomicServiceCode", "#dictType"],
        entityClass = SysDictCacheEntry::class,
        filterableProperties = ["atomicServiceCode", "dictType"]
    )
    open fun getDictByAtomicServiceCodeAndDictType(atomicServiceCode: String, dictType: String): SysDictCacheEntry? {
        return sysDictDao.fetchDictByAtomicServiceCodeAndDictType(atomicServiceCode, dictType)
    }

    // ---------- 全量刷新 ----------

    /**
     * 从库全量加载字典并刷新 Hash 缓存。
     *
     * @param clear 为 true 时先清空当前缓存再写入；为 false 时覆盖写入
     */
    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载字典 Hash 缓存")
            return
        }
        val cache = hashCache()
        val list = sysDictDao.searchAs<SysDictCacheEntry>()
        log.debug("从数据库加载 ${list.size} 条字典，刷新 Hash 缓存")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
        log.debug("字典 Hash 缓存刷新完成")
    }

    // ---------- 写库后同步（供业务在新增/更新/删除后调用） ----------

    /**
     * 新增字典后同步：将指定 id 的实体从库加载并写入缓存，并建立副属性索引。
     *
     * @param id 新增的字典主键
     */
    open fun syncOnInsert(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME) || !KeyValueCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = sysDictDao.getAs<SysDictCacheEntry>(id) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * 新增字典后同步（重载，接收业务对象与 id）。行为同 [syncOnInsert(id)]。
     *
     * @param any 业务对象，仅用于重载区分
     * @param id 新增的字典主键
     */
    open fun syncOnInsert(any: Any, id: String) {
        syncOnInsert(id)
    }

    /**
     * 更新字典后同步：将指定 id 的实体从库重新加载并写入缓存，更新副属性索引。
     *
     * @param id 被更新的字典主键
     */
    open fun syncOnUpdate(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val item = sysDictDao.getAs<SysDictCacheEntry>(id) ?: return
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /**
     * 更新字典后同步（重载，带旧 atomicServiceCode、dictType 等参数）。行为同 [syncOnUpdate(id)]。
     */
    open fun syncOnUpdate(any: Any, id: String, oldAtomicServiceCode: String?, oldDictType: String?) {
        syncOnUpdate(id)
    }

    /**
     * 更新字典启用状态后同步。行为同 [syncOnUpdate(id)]。
     *
     * @param id 字典主键
     * @param active 新的启用状态
     */
    open fun syncOnUpdateActive(id: String, active: Boolean) {
        syncOnUpdate(id)
    }

    /**
     * 删除字典后同步：从缓存中移除该 id，并从副属性 Set 索引中移除。
     *
     * @param id 被删除的字典主键
     */
    open fun syncOnDelete(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, SysDictCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * 批量删除字典后同步：从缓存中移除这些 id，并从副属性 Set 索引中移除。
     *
     * @param ids 被删除的字典主键集合
     */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val cache = hashCache()
        ids.forEach { cache.deleteById(CACHE_NAME, it, SysDictCacheEntry::class, FILTERABLE_PROPERTIES, emptySet()) }
    }

    /**
     * 生成「原子服务编码+字典类型」维度的组合 key，格式：atomicServiceCode + 分隔符 + dictType。
     * 用于外部需要与缓存 key 约定一致的场景。
     */
    fun getKeyAtomicServiceCodeAndDictType(atomicServiceCode: String, dictType: String): String {
        return "${atomicServiceCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${dictType}"
    }
}
