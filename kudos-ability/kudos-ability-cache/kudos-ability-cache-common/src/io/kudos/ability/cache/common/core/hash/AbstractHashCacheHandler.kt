package io.kudos.ability.cache.common.core.hash

import io.kudos.ability.cache.common.core.AbstractCacheHandler
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.reflect.KClass

/**
 * hash型缓存处理器抽象类
 *
 * @param T 缓存项类型（需为 [IIdEntity] 子类）
 * @author K
 * @since 1.0.0
 */
abstract class AbstractHashCacheHandler<T : IIdEntity<*>> : AbstractCacheHandler<T>() {

    private val log = LogFactory.getLog(this)

    /** 缓存实体类型，用于按 id 删除/回写时指定类型。 */
    protected abstract fun entityClass(): KClass<T>

    /** 供 [HashCacheKit] 等按 cacheName 获取实体类型，用于无类型 getValue。 */
    fun exposedEntityClass(): KClass<*> = entityClass()

    /** 用于 Set 索引的副属性名集合，与写入/删除时一致。子类按需重写。 */
    protected open fun filterableProperties(): Set<String> = emptySet()

    /** 用于 ZSet 索引的副属性名集合，与写入/删除时一致。子类按需重写。 */
    protected open fun sortableProperties(): Set<String> = emptySet()

    /**
     * 按 id 重新加载一条缓存：先从 Hash 中删除该 id，再若 [doReload] 返回非 null 则回写。
     * 子类可重写 [doReload] 实现从库/源加载；默认仅删除不回写。
     *
     * @param id 实体主键
     */
    @Suppress("UNCHECKED_CAST")
    open fun reload(id: Any) {
        if (!HashCacheKit.isCacheActive(cacheName())) return
        val cache = hashCache()
        cache.deleteById(
            cacheName(),
            id,
            entityClass() as KClass<IIdEntity<Any?>>,
            filterableProperties(),
            sortableProperties()
        )
        log.info("手动重载 Hash 缓存【${cacheName()}】，id=$id ...")
        val entity = doReload(id)
        if (entity == null) {
            log.info("数据源中已不存在对应数据。")
        } else {
            cache.saveBatch(
                cacheName(),
                listOf(entity) as List<IIdEntity<Any?>>,
                filterableProperties(),
                sortableProperties()
            )
            log.info("重载成功。")
        }
    }

    /**
     * 按 id 从数据源加载实体，供 [reload] 回写。默认返回 null（仅做删除）。
     *
     * @param id 实体主键
     * @return 加载到的实体，不存在时返回 null
     */
    protected open fun doReload(id: Any): T? = null

    /**
     * 踢除指定 id 的 Hash 缓存（仅删除，不回写）。
     *
     * @param id 实体主键
     */
    @Suppress("UNCHECKED_CAST")
    open fun evict(id: Any) {
        if (!HashCacheKit.isCacheActive(cacheName())) return
        hashCache().deleteById(
            cacheName(),
            id,
            entityClass() as KClass<IIdEntity<Any?>>,
            filterableProperties(),
            sortableProperties()
        )
    }

    protected fun hashCache() = HashCacheKit.getHashCache(cacheName())
}