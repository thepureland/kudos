package io.kudos.ms.sys.core.i18n.cache
import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.i18n.vo.SysI18nCacheEntry
import io.kudos.ms.sys.core.i18n.cache.SysI18nHashCache.Companion.CACHE_NAME
import io.kudos.ms.sys.core.i18n.cache.SysI18nHashCache.Companion.FILTERABLE_PROPERTIES
import io.kudos.ms.sys.core.i18n.dao.SysI18nDao
import jakarta.annotation.Resource
import org.springframework.stereotype.Component

/**
 * 国际化统一缓存处理器，基于 Hash 结构存储 [SysI18nCacheEntry]。
 *
 * 提供按主属性与按副属性查询与回写能力：
 *  1. 按主键 id：单条 [getI18nById]、批量 [getI18nsByIds]
 *  2. 按副属性：locale + atomicServiceCode + i18nTypeDictCode + namespace
 *
 * 数据来源表：sys_i18n
 *
 * 使用 [FILTERABLE_PROPERTIES] 中的副属性建立 Set 索引，支持多条件等值查询；所有写入、删除、全量刷新均需使用同一副属性集合以保持索引一致。
 *
 * 使用前需在缓存配置表 sys_cache 中增加名为 [CACHE_NAME] 的配置项且 hash=true。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class SysI18nHashCache : AbstractHashCacheHandler<SysI18nCacheEntry>() {

    @Resource
    private lateinit var sysI18nDao: SysI18nDao

    private val log = LogFactory.getLog(this::class)

    companion object {
        const val CACHE_NAME = "SYS_I18N__HASH"

        /** 可筛选副属性，用于按 locale / atomicServiceCode / i18nTypeDictCode / namespace 建二级索引 */
        val FILTERABLE_PROPERTIES = setOf(
            SysI18nCacheEntry::locale.name,
            SysI18nCacheEntry::atomicServiceCode.name,
            SysI18nCacheEntry::i18nTypeDictCode.name,
            SysI18nCacheEntry::namespace.name
        )
    }

    override fun cacheName(): String = CACHE_NAME

    override fun entityClass() = SysI18nCacheEntry::class

    override fun filterableProperties(): Set<String> = FILTERABLE_PROPERTIES

    override fun doReload(id: Any): SysI18nCacheEntry? =
        sysI18nDao.get(id.toString(), SysI18nCacheEntry::class)

    // ---------- 按主键 id ----------

    /**
     * 根据主键 id 从缓存获取国际化项，未命中则查库并回写。
     *
     * @param id 国际化主键，非空
     * @return 国际化缓存项，不存在时 null
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#id",
        entityClass = SysI18nCacheEntry::class,
        unless = "#result == null",
        filterableProperties = ["locale", "atomicServiceCode", "i18nTypeDictCode", "namespace"]
    )
    open fun getI18nById(id: String): SysI18nCacheEntry? {
        require(id.isNotBlank()) { "获取国际化时 id 不能为空" }
        return sysI18nDao.get(id, SysI18nCacheEntry::class)
    }

    /**
     * 根据多个主键 id 批量从缓存获取国际化项，未命中的从库加载并回写。
     *
     * @param ids 国际化主键集合，可为空
     * @return id -> 缓存对象 映射，仅包含能查到的 id
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = SysI18nCacheEntry::class,
        filterableProperties = ["locale", "atomicServiceCode", "i18nTypeDictCode", "namespace"]
    )
    open fun getI18nsByIds(ids: List<String>): Map<String, SysI18nCacheEntry> {
        if (ids.isEmpty()) return emptyMap()
        val list = sysI18nDao.getByIdsAs<SysI18nCacheEntry>(ids)
        return list.mapNotNull { e ->
            val id = e.id ?: return@mapNotNull null
            if (id.isNotBlank() && id in ids) id to e else null
        }.toMap()
    }

    // ---------- 按 locale + atomicServiceCode + i18nTypeDictCode + namespace ----------

    /**
     * 按语言、原子服务编码、国际化类型、命名空间多条件等值查询，返回匹配的启用的国际化列表。
     * 先按副属性索引查缓存，未命中则查库并回写。
     * namespace 可不传或传空，不传时按 locale + atomicServiceCode + i18nTypeDictCode 查询（不按 namespace 过滤）。
     *
     * @param locale 语言_地区，非空
     * @param atomicServiceCode 原子服务编码，非空
     * @param i18nTypeDictCode 国际化类型字典代码，非空
     * @param namespace 命名空间，缺省为null，为空不参与查询
     * @return 匹配的缓存列表
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#locale", "#atomicServiceCode", "#i18nTypeDictCode", "#namespace"],
        entityClass = SysI18nCacheEntry::class,
        filterableProperties = ["locale", "atomicServiceCode", "i18nTypeDictCode", "namespace"]
    )
    open fun getI18ns(
        locale: String,
        atomicServiceCode: String,
        i18nTypeDictCode: String,
        namespace: String? = null
    ): List<SysI18nCacheEntry> {
        require(locale.isNotBlank()) { "获取国际化时 locale 不能为空" }
        require(atomicServiceCode.isNotBlank()) { "获取国际化时 atomicServiceCode 不能为空" }
        require(i18nTypeDictCode.isNotBlank()) { "获取国际化时 i18nTypeDictCode 不能为空" }
        return sysI18nDao.fetchActiveI18nsForCache(locale, atomicServiceCode, i18nTypeDictCode, namespace ?: "")
    }

    /**
     * 按语言、原子服务编码、国际化类型、命名空间多条件等值查询，返回匹配的启用的国际化信息Map。
     * 先按副属性索引查缓存，未命中则查库并回写。
     *
     * @param locale 语言—地区，非空
     * @param atomicServiceCode 原子服务编码，非空
     * @param i18nTypeDictCode 国际化类型字典代码，非空
     * @param namespace 命名空间
     * @return Map<国际化key，译文>
     */
    open fun getI18nMap(
        locale: String,
        atomicServiceCode: String,
        i18nTypeDictCode: String,
        namespace: String
    ): Map<String, String> {
        val items = getSelf<SysI18nHashCache>().getI18ns(locale, atomicServiceCode, i18nTypeDictCode, namespace)
        return items.associateBy({ it.key }, { it.value })
    }

    /**
     * 按语言、原子服务编码、国际化类型多条件等值查询，返回匹配的启用的国际化信息Map。
     * 先按副属性索引查缓存，未命中则查库并回写。
     *
     * @param locale 语言—地区，非空
     * @param atomicServiceCode 原子服务编码，非空
     * @param i18nTypeDictCode 国际化类型字典代码，非空
     * @return Map<命名空间，Map<国际化key，译文>>
     */
    open fun getI18nMap(
        locale: String,
        atomicServiceCode: String,
        i18nTypeDictCode: String,
    ): Map<String, Map<String, String>> {
        val items = getSelf<SysI18nHashCache>().getI18ns(locale, atomicServiceCode, i18nTypeDictCode)
        return items.groupBy { it.namespace }
            .mapValues { (_, values) ->
                values.associate { it.key to it.value }
            }
    }

    // ---------- 全量刷新与同步 ----------

    /**
     * 从库全量加载启用的国际化并刷新 Hash 缓存。
     *
     * @param clear 为 true 时先清空再写入；为 false 时覆盖写入
     */
    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载国际化 Hash 缓存")
            return
        }
        val cache = hashCache()
        if (clear) cache.clear(CACHE_NAME)
        val list = sysI18nDao.fetchAllActiveI18nsForCache()
        log.debug("从数据库加载 ${list.size} 条国际化，刷新 Hash 缓存")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * 新增国际化后同步：将指定 id 的实体从库加载并写入缓存。
     *
     * @param id 主键
     */
    open fun syncOnInsert(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME) || !KeyValueCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = sysI18nDao.get(id, SysI18nCacheEntry::class) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * 新增国际化后同步（重载，接收业务对象与 id）。
     *
     * @param any 业务对象，仅用于重载区分
     * @param id 主键
     */
    open fun syncOnInsert(any: Any, id: String) {
        syncOnInsert(id)
    }

    /**
     * 更新国际化后同步：从库重新加载该 id 并写回缓存。
     *
     * @param id 主键
     */
    open fun syncOnUpdate(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val item = sysI18nDao.get(id, SysI18nCacheEntry::class) ?: return
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /**
     * 更新国际化后同步（重载）。
     *
     * @param any 业务对象，仅用于重载区分
     * @param id 主键
     */
    open fun syncOnUpdate(any: Any, id: String) {
        syncOnUpdate(id)
    }

    /**
     * 删除国际化后同步：从缓存中移除该 id 及其副属性索引。
     *
     * @param id 主键
     */
    open fun syncOnDelete(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, SysI18nCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * 批量删除后同步：从缓存中移除这些 id 及其副属性索引。
     *
     * @param ids 主键集合
     */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(cacheName())) return
        log.debug("批量删除 id 为 $ids 的 sys_i18n 后，同步从 ${cacheName()} 缓存中踢除...")
        val cache = hashCache()
        ids.forEach {
            cache.deleteById(cacheName(), it, SysI18nCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
        }
        log.debug("${cacheName()} 缓存同步完成。")
    }
}
