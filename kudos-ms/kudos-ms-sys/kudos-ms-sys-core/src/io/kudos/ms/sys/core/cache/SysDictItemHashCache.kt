package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheEntry
import io.kudos.ms.sys.core.cache.SysDictItemHashCache.Companion.CACHE_NAME
import io.kudos.ms.sys.core.cache.SysDictItemHashCache.Companion.FILTERABLE_PROPERTIES
import io.kudos.ms.sys.core.dao.VSysDictItemDao
import jakarta.annotation.Resource
import org.springframework.stereotype.Component

/**
 * 字典项统一缓存处理器，基于 Hash 结构存储 [SysDictItemCacheEntry]。
 *
 * 数据来源：视图 v_sys_dict_item（sys_dict_item left join sys_dict）。
 *
 * 提供按主属性与按副属性查询与回写能力：
 * - **按主键 id**：单条、批量。
 * - **按副属性**：atomicServiceCode + dictType + itemCode（单条）；atomicServiceCode + dictType（列表）；parentId（子项列表）。
 *
 * 使用 [FILTERABLE_PROPERTIES] 中的副属性建立 Set 索引，支持多条件等值查询；所有写入、删除、全量刷新均需使用同一副属性集合以保持索引一致。
 *
 * 使用前需在缓存配置表 sys_cache 中增加名为 [CACHE_NAME] 的配置项且 hash=true。
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysDictItemHashCache : AbstractHashCacheHandler<SysDictItemCacheEntry>() {

    @Resource
    private lateinit var vSysDictItemDao: VSysDictItemDao

    private val log = LogFactory.getLog(this)

    companion object {
        const val CACHE_NAME = "SYS_DICT_ITEM__HASH"

        /** 用于等值筛选与 Set 索引的副属性名集合，写入/删除/全量刷新时须与此一致 */
        val FILTERABLE_PROPERTIES = setOf(
            SysDictItemCacheEntry::atomicServiceCode.name,
            SysDictItemCacheEntry::dictType.name,
            SysDictItemCacheEntry::itemCode.name,
            SysDictItemCacheEntry::parentId.name
        )
    }

    override fun cacheName(): String = CACHE_NAME

    override fun entityClass() = SysDictItemCacheEntry::class

    override fun filterableProperties(): Set<String> = FILTERABLE_PROPERTIES

    override fun doReload(id: Any): SysDictItemCacheEntry? = vSysDictItemDao.getAs<SysDictItemCacheEntry>(id.toString())?.trimmed()

    // ---------- 1. 按主键 id ----------

    /**
     * 根据主键 id 获取单条字典项实体。
     * 先查缓存，未命中则查视图并回写；回写时按 [FILTERABLE_PROPERTIES] 建立副属性索引。
     *
     * @param id 字典项主键，非空
     * @return 字典项缓存项，不存在时 null
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#id",
        entityClass = SysDictItemCacheEntry::class,
        unless = "#result == null",
        filterableProperties = ["atomicServiceCode", "dictType", "itemCode", "parentId"]
    )
    open fun getDictItemById(id: String): SysDictItemCacheEntry? {
        require(id.isNotBlank()) { "获取字典项时 id 不能为空" }
        return vSysDictItemDao.getAs<SysDictItemCacheEntry>(id)?.trimmed()
    }

    /**
     * 根据主键 id 列表批量获取字典项实体。
     * 先查缓存，未命中的 id 再查视图并回写；回写时按 [FILTERABLE_PROPERTIES] 建立副属性索引。
     *
     * @param ids 字典项主键列表，可为空
     * @return id -> 实体 映射，仅包含能查到的 id
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = SysDictItemCacheEntry::class,
        filterableProperties = ["atomicServiceCode", "dictType", "itemCode", "parentId"]
    )
    open fun getDictItemsByIds(ids: Set<String>): Map<String, SysDictItemCacheEntry> {
        if (ids.isEmpty()) return emptyMap()
        val list = vSysDictItemDao.getByIdsAs<SysDictItemCacheEntry>(ids).map { it.trimmed() }
        val byId = list.associateBy { it.id }
        return ids.mapNotNull { id -> byId[id]?.let { id to it } }.toMap()
    }

    // ---------- 2. 按 atomicServiceCode + dictType + itemCode ----------

    /**
     * 按原子服务编码、字典类型、字典项代码及启用状态多条件等值查询，返回匹配的字典项（0 或 1 条）。
     * 先按副属性索引查缓存，未命中则查视图并回写。
     *
     * @param atomicServiceCode 原子服务编码，非空
     * @param dictType 字典类型，非空
     * @param itemCode 字典项代码，非空
     * @return 匹配的字典项实体，不存在时 null
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#atomicServiceCode", "#dictType", "#itemCode"],
        entityClass = SysDictItemCacheEntry::class,
        filterableProperties = ["atomicServiceCode", "dictType", "itemCode", "parentId"]
    )
    open fun getDictItem(
        atomicServiceCode: String,
        dictType: String,
        itemCode: String
    ): SysDictItemCacheEntry? {
        return vSysDictItemDao.fetchByAtomicServiceCodeAndDictTypeAndItemCode(
            atomicServiceCode,
            dictType,
            itemCode
        )?.trimmed()
    }

    // ---------- 3. 按 atomicServiceCode + dictType ----------

    /**
     * 按原子服务编码、字典类型及启用状态多条件等值查询，返回匹配的字典项列表（按 orderNum 排序）。
     * 先按副属性索引查缓存，未命中则查视图并回写。
     *
     * @param atomicServiceCode 原子服务编码，非空
     * @param dictType 字典类型，非空
     * @return 匹配的字典项实体列表
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#atomicServiceCode", "#dictType"],
        entityClass = SysDictItemCacheEntry::class,
        filterableProperties = ["atomicServiceCode", "dictType", "itemCode", "parentId"]
    )
    open fun getDictItems(
        atomicServiceCode: String,
        dictType: String
    ): List<SysDictItemCacheEntry> {
        return vSysDictItemDao.searchByAtomicServiceCodeAndDictType(atomicServiceCode, dictType).map { it.trimmed() }
    }

    // ---------- 4. 按 parentId ----------

    /**
     * 按父字典项 id 及启用状态查询子字典项列表（按 orderNum 排序）。
     * 先按副属性索引查缓存，未命中则查视图并回写。
     *
     * @param parentId 父字典项 id，非空
     * @return 匹配的字典项实体列表
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#parentId"],
        entityClass = SysDictItemCacheEntry::class,
        filterableProperties = ["atomicServiceCode", "dictType", "itemCode", "parentId"]
    )
    open fun getDictItems(parentId: String): List<SysDictItemCacheEntry> {
        require(parentId.isNotBlank()) { "获取子字典项时 parentId 不能为空" }
        return vSysDictItemDao.searchByParentId(parentId).map { it.trimmed() }
    }

    // ---------- 全量刷新 ----------

    /**
     * 从视图全量加载字典项并刷新 Hash 缓存。
     *
     * @param clear 为 true 时先清空当前缓存再写入；为 false 时覆盖写入
     */
    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载字典项 Hash 缓存")
            return
        }
        val cache = hashCache()
        val list = vSysDictItemDao.searchAs<SysDictItemCacheEntry>().map { it.trimmed() }
        log.debug("从视图 v_sys_dict_item 加载 ${list.size} 条字典项，刷新 Hash 缓存")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
        log.debug("字典项 Hash 缓存刷新完成")
    }

    // ---------- 写库后同步（供业务在 sys_dict_item 新增/更新/删除后调用） ----------

    /**
     * 新增字典项后同步：将指定 id 的实体从视图加载并写入缓存，并建立副属性索引。
     *
     * @param id 新增的字典项主键
     */
    open fun syncOnInsert(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME) || !KeyValueCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = vSysDictItemDao.getAs<SysDictItemCacheEntry>(id)?.trimmed() ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * 新增字典项后同步（重载，接收业务对象与 id）。行为同 [syncOnInsert(id)]。
     */
    open fun syncOnInsert(any: Any, id: String) {
        syncOnInsert(id)
    }

    /**
     * 更新字典项后同步：将指定 id 的实体从视图重新加载并写入缓存，更新副属性索引。
     *
     * @param id 被更新的字典项主键
     */
    open fun syncOnUpdate(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val item = vSysDictItemDao.getAs<SysDictItemCacheEntry>(id)?.trimmed() ?: return
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /**
     * 更新字典项后同步（重载，带旧副属性参数）。行为同 [syncOnUpdate(id)]。
     */
    open fun syncOnUpdate(
        any: Any,
        id: String,
        oldAtomicServiceCode: String?,
        oldDictType: String?,
        oldItemCode: String?
    ) {
        syncOnUpdate(id)
    }

    /**
     * 删除字典项后同步：从缓存中移除该 id，并从副属性 Set 索引中移除。
     *
     * @param id 被删除的字典项主键
     * @param atomicServiceCode 该字典项所属原子服务编码（用于索引移除）
     * @param dictType 字典类型（用于索引移除），可为 null
     * @param itemCode 字典项代码（用于索引移除），可为 null
     */
    open fun syncOnDelete(id: String, atomicServiceCode: String, dictType: String?, itemCode: String?) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, SysDictItemCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * 批量删除字典项后同步：从缓存中移除这些 id，并从副属性 Set 索引中移除。
     *
     * @param ids 被删除的字典项主键集合
     */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val cache = hashCache()
        ids.forEach { cache.deleteById(CACHE_NAME, it, SysDictItemCacheEntry::class, FILTERABLE_PROPERTIES, emptySet()) }
    }

    /**
     * 生成「原子服务编码+字典类型+字典项代码」维度的组合 key。
     * 用于外部需要与缓存 key 约定一致的场景。
     */
    fun getKeyAtomicServiceCodeAndDictTypeAndItemCode(
        atomicServiceCode: String,
        dictType: String,
        itemCode: String
    ): String {
        return "${atomicServiceCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${dictType}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${itemCode}"
    }

    /**
     * 生成「原子服务编码+字典类型」维度的组合 key。
     */
    fun getKeyAtomicServiceCodeAndDictType(atomicServiceCode: String, dictType: String): String {
        return "${atomicServiceCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${dictType}"
    }
}
