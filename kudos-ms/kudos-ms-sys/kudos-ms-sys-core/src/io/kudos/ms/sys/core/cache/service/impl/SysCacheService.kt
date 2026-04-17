package io.kudos.ms.sys.core.cache.service.impl

import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate

import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.data.json.JsonKit
import io.kudos.base.error.ServiceException
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.ms.sys.common.cache.enums.SysCacheErrorCodeEnum
import io.kudos.ms.sys.common.cache.vo.SysCacheCacheEntry
import io.kudos.ms.sys.core.cache.cache.SysCacheHashCache
import io.kudos.ms.sys.core.cache.dao.SysCacheDao
import io.kudos.ms.sys.core.cache.model.po.SysCache
import io.kudos.ms.sys.core.cache.service.iservice.ISysCacheService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass


/**
 * 缓存业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class SysCacheService(
    dao: SysCacheDao,
    private val sysCacheHashCache: SysCacheHashCache,
) : BaseCrudService<String, SysCache, SysCacheDao>(dao), ISysCacheService {

    private val log = LogFactory.getLog(this::class)

    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        return if (returnType == SysCacheCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            sysCacheHashCache.getCacheById(id) as R?
        } else {
            super.get(id, returnType)
        }
    }

    override fun getCacheFromCache(id: String): SysCacheCacheEntry? = sysCacheHashCache.getCacheById(id)

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "新增id为${id}的缓存配置。") {
            sysCacheHashCache.syncOnInsert(any, id)
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireCacheId(any)
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "更新id为${id}的缓存配置。",
            failureMessage = "更新id为${id}的缓存配置失败！",
        ) {
            sysCacheHashCache.syncOnUpdate(any, id)
        }
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val cache = SysCache {
            this.id = id
            this.active = active
        }
        return completeCrudUpdate(
            success = dao.update(cache),
            log = log,
            successMessage = "更新id为${id}的缓存配置的启用状态为${active}。",
            failureMessage = "更新id为${id}的缓存配置的启用状态为${active}失败！",
        ) {
            sysCacheHashCache.syncOnUpdate(cache, id)
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val sysCache = dao.get(id)
        if (sysCache == null) {
            log.warn("删除id为${id}的缓存配置时，发现其已不存在！")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "删除id为${id}的缓存配置成功！",
            failureMessage = "删除id为${id}的缓存配置失败！",
        ) {
            sysCacheHashCache.syncOnDelete(id)
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除缓存配置，期望删除${ids.size}条，实际删除${count}条。")
        sysCacheHashCache.syncOnBatchDelete(ids)
        return count
    }

    override fun getCachesFromCache(atomicServiceCode: String): List<SysCacheCacheEntry> = sysCacheHashCache.getCaches(atomicServiceCode)

    override fun reload(id: String, key: String) {
        require(id.isNotBlank())
        require(key.isNotBlank())
        withCacheConfig(id) { cache ->
            val name = cache.name
            if (isKeyValueCache(cache)) {
                requireKeyExists(name, key, keyValueCache = true)
                KeyValueCacheKit.reload(name, key)
            } else {
                requireKeyExists(name, key, keyValueCache = false)
                HashCacheKit.reload(name, key)
            }
        }
    }

    override fun reloadAll(id: String) {
        require(id.isNotBlank())
        withCacheConfig(id) { cache ->
            val name = cache.name
            if (isKeyValueCache(cache)) {
                KeyValueCacheKit.reloadAll(name)
            } else {
                HashCacheKit.reloadAll(name)
            }
        }
    }

    override fun evict(id: String, key: String) {
        require(id.isNotBlank())
        require(key.isNotBlank())
        withCacheConfig(id) { cache ->
            val name = cache.name
            if (isKeyValueCache(cache)) {
                requireKeyExists(name, key, keyValueCache = true)
                KeyValueCacheKit.evict(name, key)
            } else {
                requireKeyExists(name, key, keyValueCache = false)
                HashCacheKit.evict(name, key)
            }
        }
    }

    override fun evictAll(id: String) {
        require(id.isNotBlank())
        withCacheConfig(id) { cache ->
            val name = cache.name
            if (isKeyValueCache(cache)) {
                KeyValueCacheKit.clear(name)
            } else {
                HashCacheKit.clear(name)
            }
        }
    }

    override fun existsKey(id: String, key: String): Boolean {
        require(id.isNotBlank())
        require(key.isNotBlank())
        return withCacheConfig(id) { cache ->
            existsKey(cache.name, key, isKeyValueCache(cache))
        }
    }

    override fun getValueJson(id: String, key: String): String {
        require(id.isNotBlank())
        require(key.isNotBlank())
        val value = withCacheConfig(id) { cache ->
            val name = cache.name
            if (isKeyValueCache(cache)) {
                requireKeyExists(name, key, keyValueCache = true)
                KeyValueCacheKit.getValue(name, key)
            } else {
                requireKeyExists(name, key, keyValueCache = false)
                HashCacheKit.getValue(name, key)
            }
        }
        return JsonKit.toJson(value)
    }

    /** 按 id 获取缓存配置，不存在则抛异常 */
    private fun getCacheConfigById(id: String): SysCacheCacheEntry {
        return sysCacheHashCache.getCacheById(id)
            ?: throw ServiceException(SysCacheErrorCodeEnum.CACHE_CONFIG_NOT_FOUND)
    }

    private fun requireKeyExists(name: String, key: String, keyValueCache: Boolean) {
        if (!existsKey(name, key, keyValueCache)) {
            throw ServiceException(SysCacheErrorCodeEnum.CACHE_KEY_NOT_FOUND)
        }
    }

    private fun existsKey(name: String, key: String, keyValueCache: Boolean): Boolean {
        return if (keyValueCache) {
            KeyValueCacheKit.existsKey(name, key)
        } else {
            HashCacheKit.existsById(name, key)
        }
    }

    private inline fun <T> withCacheConfig(id: String, block: (SysCacheCacheEntry) -> T): T =
        block(getCacheConfigById(id))

    private fun requireCacheId(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("更新缓存配置时不支持的入参类型: ${any::class.qualifiedName}")

    private fun isKeyValueCache(cache: SysCacheCacheEntry): Boolean = !cache.hash
}
