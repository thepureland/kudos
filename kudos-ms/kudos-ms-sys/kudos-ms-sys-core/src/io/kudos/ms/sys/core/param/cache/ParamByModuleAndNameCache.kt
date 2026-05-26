package io.kudos.ms.sys.core.param.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.context.support.Consts
import io.kudos.ms.sys.common.param.vo.SysParamCacheEntry
import io.kudos.ms.sys.core.param.dao.SysParamDao
import io.kudos.ms.sys.core.param.event.SysParamBatchDeleted
import io.kudos.ms.sys.core.param.event.SysParamDeleted
import io.kudos.ms.sys.core.param.event.SysParamInserted
import io.kudos.ms.sys.core.param.event.SysParamUpdated
import io.kudos.ms.sys.core.param.model.po.SysParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * Parameter (by module & name) cache handler.
 *
 * 1. Source table: sys_param.
 * 2. Caches every parameter with active=true.
 * 3. Cache key: atomicServiceCode::paramName
 * 4. Cache value: SysParamCacheEntry
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class ParamByModuleAndNameCache : AbstractKeyValueCacheHandler<SysParamCacheEntry>() {

    @Autowired
    private lateinit var sysParamDao: SysParamDao

    companion object {
        private const val CACHE_NAME = "SYS_PARAM_BY_MODULE_AND_NAME"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): SysParamCacheEntry? {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "Cache $CACHE_NAME key format must be moduleCode${Consts.CACHE_KEY_DEFAULT_DELIMITER}paramName"
        }
        val moduleAndParamName = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return getSelf<ParamByModuleAndNameCache>().getParam(
            moduleAndParamName[0], moduleAndParamName[1]
        )
    }

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache is not active; skipping load of all enabled parameters!")
            return
        }

        // Load all enabled parameters
        val criteria = Criteria(SysParam::active eq true)
        val params = sysParamDao.searchAs<SysParamCacheEntry>(criteria)
        log.debug("Loaded ${params.size} parameter records from DB.")

        // Clear cache
        if (clear) {
            clear()
        }

        // Cache parameters
        params.forEach {
            val atomicServiceCode = it.atomicServiceCode
            val paramName = it.paramName
            KeyValueCacheKit.put(CACHE_NAME, getKey(atomicServiceCode, paramName), it)
        }
        log.debug("Cached ${params.size} parameter records.")
    }

    /**
     * Fetch parameter info by module code and parameter name from cache; on miss, load from DB and write back to cache.
     *
     * @param atomicServiceCode module code
     * @param paramName parameter name
     * @return SysParamCacheEntry, or null if not found
     */
    @Cacheable(
        value = [CACHE_NAME],
        key = "#atomicServiceCode.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#paramName)",
        unless = "#result == null"
    )
    open fun getParam(atomicServiceCode: String, paramName: String): SysParamCacheEntry? {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("No cached parameter for module=$atomicServiceCode, name=$paramName; loading from DB...")
        }
        val param = sysParamDao.getActiveParamsForCache(atomicServiceCode, paramName)
        if (param == null) {
            log.warn("No active=true parameter found in DB for module=$atomicServiceCode, name=$paramName!")
        }
        return param
    }

    /**
     * Sync cache after a DB insert.
     *
     * @param any object holding the required properties
     * @param id parameter id
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME) && KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            log.debug("After inserting parameter id=$id, syncing $CACHE_NAME cache...")
            val atomicServiceCode = BeanKit.getProperty(any, SysParam::atomicServiceCode.name) as String
            val paramName = BeanKit.getProperty(any, SysParam::paramName.name) as String
            getSelf<ParamByModuleAndNameCache>().getParam(atomicServiceCode, paramName) // populate cache
            log.debug("$CACHE_NAME cache sync complete.")
        }
    }

    /**
     * Sync cache after a DB update.
     *
     * @param any object holding the required properties
     * @param id parameter id
     */
    open fun syncOnUpdate(any: Any, id: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After updating parameter id=$id, syncing $CACHE_NAME cache...")
            val atomicServiceCode = BeanKit.getProperty(any, SysParam::atomicServiceCode.name) as String
            val paramName = BeanKit.getProperty(any, SysParam::paramName.name) as String
            KeyValueCacheKit.evict(CACHE_NAME, getKey(atomicServiceCode, paramName)) // evict parameter cache
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<ParamByModuleAndNameCache>().getParam(atomicServiceCode, paramName) // re-cache
            }
            log.debug("$CACHE_NAME cache sync complete.")
        }
    }

    /**
     * Sync cache after the enabled state is updated.
     *
     * @param id parameter id
     * @param active whether enabled
     */
    open fun syncOnUpdateActive(id: String, active: Boolean) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After updating active state of parameter id=$id, syncing cache...")
            val sysParam = requireNotNull(sysParamDao.get(id)) { "Cannot find parameter record id=$id when syncing active-state cache." }
            if (active) {
                if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<ParamByModuleAndNameCache>().getParam(
                        sysParam.atomicServiceCode, sysParam.paramName
                    )
                }
            } else {
                KeyValueCacheKit.evict(CACHE_NAME, getKey(sysParam.atomicServiceCode, sysParam.paramName)) // evict parameter cache
            }
            log.debug("Cache sync complete.")
        }
    }

    /**
     * Sync cache after a DB delete.
     *
     * @param any object holding the required properties
     * @param id parameter id
     */
    open fun syncOnDelete(any: Any, id: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            val atomicServiceCode = BeanKit.getProperty(any, SysParam::atomicServiceCode.name) as String
            val paramName = BeanKit.getProperty(any, SysParam::paramName.name) as String
            log.debug("After deleting parameter id=$id, evicting from $CACHE_NAME cache...")
            KeyValueCacheKit.evict(CACHE_NAME, getKey(atomicServiceCode, paramName)) // evict cache
            log.debug("$CACHE_NAME cache sync complete.")
        }
    }

    /**
     * Sync cache after batch deletion of DB records.
     *
     * @param ids parameter id collection
     * @param moduleAndNames List<Pair<atomicServiceCode, paramName>>
     */
    open fun syncOnBatchDelete(ids: Collection<String>, moduleAndNames: List<Pair<String, String>>) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After batch-deleting parameters ids=$ids, evicting from $CACHE_NAME cache...")
            moduleAndNames.forEach {
                KeyValueCacheKit.evict(CACHE_NAME, getKey(it.first, it.second)) // evict cache
            }
            log.debug("$CACHE_NAME cache sync complete.")
        }
    }

    /**
     * Build the composite cache key for a parameter.
     *
     * @param atomicServiceCode atomic service code
     * @param paramName parameter name
     * @return cache key
     */
    fun getKey(atomicServiceCode: String, paramName: String): String {
        return "${atomicServiceCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${paramName}"
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysParamInserted) {
        // After AFTER_COMMIT the DB row is visible; look up dims by id directly to avoid requiring module+name on the event.
        val param = sysParamDao.get(event.id) ?: return
        syncOnInsert(param, event.id)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysParamUpdated) {
        val param = sysParamDao.get(event.id) ?: return
        syncOnUpdate(param, event.id)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysParamDeleted) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, getKey(event.atomicServiceCode, event.paramName))
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysParamBatchDeleted): Unit =
        syncOnBatchDelete(event.ids, event.moduleAndNames)

    private val log = LogFactory.getLog(this::class)

}
