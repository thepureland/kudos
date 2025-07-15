package io.kudos.ability.cache.common.support

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.base.lang.GenericKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.IIdEntity
import io.kudos.base.support.dao.IBaseReadOnlyDao
import io.kudos.base.support.payload.ListSearchPayload
import org.springframework.beans.factory.annotation.Autowired
import kotlin.reflect.KClass


/**
 * key为id的缓存处理器抽象类
 *
 * @param T 值类型
 * @author K
 * @since 1.0.0
 */
abstract class AbstractByIdCacheHandler<PK: Any, T: IIdEntity<*>, DAO: IBaseReadOnlyDao<PK, *>>: AbstractCacheHandler<T>() {

    @Autowired
    protected lateinit var dao: DAO

    protected fun getById(id: PK): T? {
        if (id is CharSequence) {
            require(id.isNotEmpty()) { log.error("从${cacheName()}缓存中获取${itemDesc()}时，id不能为空！") }
        }
        if (CacheKit.isCacheActive(cacheName())) {
            log.debug("缓存中不存在id为${id}的${itemDesc()}，从数据库中加载...")
        }
        val result = dao.get(id, getCacheItemClass())
        if (result == null) {
            log.warn("数据库中不存在id为${id}的${itemDesc()}！")
        } else {
            log.debug("数据库加载到id为${id}的${itemDesc()}.")
        }
        return result
    }

    protected fun getByIds(ids: Collection<PK>): Map<String, T> {
        require(ids.isNotEmpty()) { log.error("批量从${cacheName()}缓存中获取${itemDesc()}时，id集合不能为空！") }
        if (CacheKit.isCacheActive(cacheName())) {
            log.debug("${cacheName()}缓存中没有找到所有这些id为${ids}的${itemDesc()}，从数据库中加载...")
        }
        val searchPayload = ListSearchPayload().apply {
            returnEntityClass = getCacheItemClass()
            criterions = listOf(Criterion("id", OperatorEnum.IN, ids))
        }

        @Suppress("UNCHECKED_CAST")
        val results = dao.search(searchPayload) as List<T>
        log.debug("数据库中加载到${results.size}条${itemDesc()}.")
        return results.associateBy { it.id!!.toString() }
    }

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(cacheName())) {
            log.info("缓存未开启，不加载和缓存所有${itemDesc()}信息！")
            return
        }

        // 加载所有
        val searchPayload = ListSearchPayload().apply {
            returnEntityClass = getCacheItemClass()
        }

        @Suppress("UNCHECKED_CAST")
        val results = dao.search(searchPayload) as List<T>
        log.debug("从数据库加载了${results.size}条${itemDesc()}信息。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存
        results.forEach {
            CacheKit.putIfAbsent(cacheName(), it.id!!, it)
        }
        log.debug("缓存了${results.size}条${itemDesc()}信息。")
    }

    open fun syncOnInsert(id: PK) {
        if (CacheKit.isCacheActive(cacheName()) && CacheKit.isWriteInTime(cacheName())) {
            log.debug("新增id为${id}的${itemDesc()}后，同步${cacheName()}缓存...")
            doReload(id.toString()) // 缓存
            log.debug("${cacheName()}缓存同步完成。")
        }
    }

    open fun syncOnUpdate(id: PK) {
        if (CacheKit.isCacheActive(cacheName())) {
            log.debug("更新id为${id}的${itemDesc()}后，同步${cacheName()}缓存...")
            CacheKit.evict(cacheName(), id) // 踢除缓存
            if (CacheKit.isWriteInTime(cacheName())) {
                doReload(id.toString()) // 缓存
            }
            log.debug("${cacheName()}缓存同步完成。")
        }
    }

    open fun syncOnDelete(id: PK) {
        if (CacheKit.isCacheActive(cacheName())) {
            log.debug("删除id为${id}的${itemDesc()}后，同步${cacheName()}缓存...")
            CacheKit.evict(cacheName(), id) // 踢除缓存
            log.debug("${cacheName()}缓存同步完成。")
        }
    }

    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (CacheKit.isCacheActive(cacheName())) {
            log.debug("批量删除id为${ids}的${itemDesc()}后，同步从${cacheName()}缓存中踢除...")
            ids.forEach {
                CacheKit.evict(cacheName(), it) // 踢除角色缓存
            }
            log.debug("${cacheName()}缓存同步完成。")
        }
    }

    protected fun getCacheItemClass(): KClass<T> {
        @Suppress("UNCHECKED_CAST")
        return GenericKit.getSuperClassGenricClass(this::class, 1) as KClass<T>
    }

    protected abstract fun itemDesc(): String

    private val log = LogFactory.getLog(this)
    
}