package io.kudos.ms.sys.core.locale.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.locale.vo.SysLocaleCacheEntry
import io.kudos.ms.sys.core.locale.dao.SysLocaleDao
import io.kudos.ms.sys.core.locale.event.SysLocaleBatchDeleted
import io.kudos.ms.sys.core.locale.event.SysLocaleDeleted
import io.kudos.ms.sys.core.locale.event.SysLocaleInserted
import io.kudos.ms.sys.core.locale.event.SysLocaleUpdated
import io.kudos.ms.sys.core.locale.model.po.SysLocale
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * 语言/区域字典缓存处理器
 *
 * 1.数据来源表：sys_locale
 * 2.仅缓存 active=true 的语言
 * 3.缓存 key：locale code（如 zh_CN）
 * 4.缓存 value：[SysLocaleCacheEntry]
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class LocaleByCodeCache : AbstractKeyValueCacheHandler<SysLocaleCacheEntry>() {

    @Autowired
    private lateinit var dao: SysLocaleDao

    companion object {
        private const val CACHE_NAME = "SYS_LOCALE_BY_CODE"
    }

    override fun cacheName() = CACHE_NAME

    override fun doReload(key: String): SysLocaleCacheEntry? {
        return getSelf<LocaleByCodeCache>().getLocale(key)
    }

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有语言信息！")
            return
        }
        val criteria = Criteria(SysLocale::active eq true)
        val locales = dao.searchAs<SysLocaleCacheEntry>(criteria)
        log.debug("从数据库加载了${locales.size}条语言信息。")
        if (clear) clear()
        locales.forEach { KeyValueCacheKit.put(CACHE_NAME, it.code, it) }
        log.debug("缓存了${locales.size}条语言信息。")
    }

    /**
     * 按 code 取语言；缓存未命中则查库回填。
     *
     * @param code 语言代码
     * @return 缓存项；查无结果或未启用返回 null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#code",
        unless = "#result == null"
    )
    open fun getLocale(code: String): SysLocaleCacheEntry? {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在 code=${code} 的语言，从数据库中加载...")
        }
        val criteria = Criteria.and(
            SysLocale::code eq code,
            SysLocale::active eq true,
        )
        val locales = dao.searchAs<SysLocaleCacheEntry>(criteria)
        return if (locales.isEmpty()) {
            log.debug("从数据库找不到 active=true 且 code=${code} 的语言。")
            null
        } else {
            log.debug("从数据库加载了 code=${code} 的语言。")
            locales.first()
        }
    }

    /** 数据库插入记录后同步缓存 */
    open fun syncOnInsert(any: Any, id: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME) && KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            val code = BeanKit.getProperty(any, SysLocale::code.name) as String
            log.debug("新增 id=${id} 的语言后，同步 ${CACHE_NAME} 缓存...")
            KeyValueCacheKit.evict(CACHE_NAME, code)
            getSelf<LocaleByCodeCache>().getLocale(code)
        }
    }

    /** 数据库更新记录后同步缓存 */
    open fun syncOnUpdate(any: Any?, id: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            val code = if (any == null) dao.get(id)?.code else BeanKit.getProperty(any, SysLocale::code.name) as String
            if (code.isNullOrBlank()) return
            log.debug("更新 id=${id} 的语言后，同步 ${CACHE_NAME} 缓存...")
            KeyValueCacheKit.evict(CACHE_NAME, code)
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<LocaleByCodeCache>().getLocale(code)
            }
        }
    }

    /** 数据库删除记录后同步缓存 */
    open fun syncOnDelete(code: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除 code=${code} 的语言后，从 ${CACHE_NAME} 缓存中踢除...")
            KeyValueCacheKit.evict(CACHE_NAME, code)
        }
    }

    /** 批量删除后同步缓存 */
    open fun syncOnBatchDelete(codes: Set<String>) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量删除语言后，从 ${CACHE_NAME} 缓存中踢除 ${codes.size} 条...")
            codes.forEach { KeyValueCacheKit.evict(CACHE_NAME, it) }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysLocaleInserted) {
        val po = dao.get(event.id) ?: return
        syncOnInsert(po, event.id)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysLocaleUpdated): Unit = syncOnUpdate(null, event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysLocaleDeleted): Unit = syncOnDelete(event.code)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysLocaleBatchDeleted): Unit = syncOnBatchDelete(event.codes)

    private val log = LogFactory.getLog(this::class)

}
