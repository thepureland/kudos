package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import io.kudos.ms.sys.core.dao.SysI18nDao
import io.kudos.ms.sys.core.model.po.SysI18n
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

/**
 * 国际化信息缓存处理器
 *
 * 1.数据来源表：sys_i18n
 * 2.仅缓存active=true的
 * 3.缓存key为：语言::类型::原子服务
 * 4.缓存value为：Map<国际化key, 国际化value>
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class I18NByLocaleAndTypeAndAmsCodeCache : AbstractKeyValueCacheHandler<Map<String, String>>() {

    @Autowired
    private lateinit var sysI18nDao: SysI18nDao

    companion object {
        private const val CACHE_NAME = "I18N_BY_LOCALE_AND_TYPE_AND_AMS_CODE"
    }

    override fun cacheName() = CACHE_NAME

    override fun doReload(key: String): Map<String, String>? {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "缓存${CACHE_NAME}的key格式必须是：语言${Consts.CACHE_KEY_DEFAULT_DELIMITER}类型${Consts.CACHE_KEY_DEFAULT_DELIMITER}原子服务编码"
        }
        val parts = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        require(parts.size == 3) {
            "缓存${CACHE_NAME}的key格式必须是：语言${Consts.CACHE_KEY_DEFAULT_DELIMITER}类型${Consts.CACHE_KEY_DEFAULT_DELIMITER}原子服务编码"
        }
        return getSelf<I18NByLocaleAndTypeAndAmsCodeCache>().getI18ns(parts[0], parts[1], parts[2])
    }

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有启用状态的国际化内容！")
            return
        }

        val results = sysI18nDao.fetchAllActiveI18nsForCache()
        log.debug("从数据库加载了${results.size}条国际化内容。")

        if (clear) {
            clear()
        }

        val grouped = results.groupBy {
            getKey(it.locale, it.i18nTypeDictCode, it.atomicServiceCode)
        }
        grouped.forEach { (key, items) ->
            val valueMap = items.mapNotNull { item ->
                val i18nKey = item.key ?: return@mapNotNull null
                val i18nValue = item.value ?: return@mapNotNull null
                i18nKey to i18nValue
            }.toMap()
            CacheKit.put(CACHE_NAME, key, valueMap)
        }
        log.debug("缓存了${results.size}条国际化内容。")
    }

    /**
     * 根据语言、类型、原子服务编码获取国际化内容，如果缓存中不存在，则从数据库加载并写入缓存。
     *
     * @param locale 语言_地区
     * @param i18nTypeDictCode 国际化类型字典代码
     * @param atomicServiceCode 原子服务编码
     * @return Map<国际化key, 国际化value>，找不到返回空Map
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#locale.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}')" +
            ".concat(#i18nTypeDictCode).concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#atomicServiceCode)",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getI18ns(locale: String, i18nTypeDictCode: String, atomicServiceCode: String): Map<String, String> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在语言为${locale}、类型为${i18nTypeDictCode}且原子服务为${atomicServiceCode}的国际化内容，从数据库中加载...")
        }
        val items = sysI18nDao.fetchActiveI18nsForCache(locale, i18nTypeDictCode, atomicServiceCode)
        if (items.isEmpty()) {
            log.warn("数据库中不存在语言为${locale}、类型为${i18nTypeDictCode}且原子服务为${atomicServiceCode}的active=true的国际化内容！")
            return emptyMap()
        }
        val map = items.mapNotNull { item ->
            val i18nKey = item.key ?: return@mapNotNull null
            val i18nValue = item.value ?: return@mapNotNull null
            i18nKey to i18nValue
        }.toMap()
        log.debug("数据库中加载到语言为${locale}、类型为${i18nTypeDictCode}且原子服务为${atomicServiceCode}的国际化内容共${map.size}条。")
        return map
    }

    /**
     * 数据库插入记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 国际化id
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("新增id为${id}的国际化内容后，同步${CACHE_NAME}缓存...")
            val (locale, i18nTypeDictCode, atomicServiceCode) = resolveKeyParts(any, id) ?: return
            val cacheKey = getKey(locale, i18nTypeDictCode, atomicServiceCode)
            CacheKit.evict(CACHE_NAME, cacheKey)
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<I18NByLocaleAndTypeAndAmsCodeCache>().getI18ns(
                    locale, i18nTypeDictCode, atomicServiceCode
                )
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 更新数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 国际化id
     */
    open fun syncOnUpdate(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的国际化内容后，同步${CACHE_NAME}缓存...")
            val (locale, i18nTypeDictCode, atomicServiceCode) = resolveKeyParts(any, id) ?: return
            val cacheKey = getKey(locale, i18nTypeDictCode, atomicServiceCode)
            CacheKit.evict(CACHE_NAME, cacheKey)
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<I18NByLocaleAndTypeAndAmsCodeCache>().getI18ns(
                    locale, i18nTypeDictCode, atomicServiceCode
                )
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 更新启用状态后同步缓存
     *
     * @param id 国际化id
     * @param active 是否启用
     */
    open fun syncOnUpdateActive(id: String, active: Boolean) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的国际化内容的启用状态后，同步${CACHE_NAME}缓存...")
            val i18n = sysI18nDao.get(id)
            if (i18n == null) {
                log.warn("同步国际化缓存时未找到id为${id}的记录。")
                return
            }
            val cacheKey = getKey(i18n.locale, i18n.i18nTypeDictCode, i18n.atomicServiceCode)
            if (active) {
                CacheKit.evict(CACHE_NAME, cacheKey)
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<I18NByLocaleAndTypeAndAmsCodeCache>().getI18ns(
                        i18n.locale, i18n.i18nTypeDictCode, i18n.atomicServiceCode
                    )
                }
            } else {
                CacheKit.evict(CACHE_NAME, cacheKey)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 删除数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 国际化id
     */
    open fun syncOnDelete(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除id为${id}的国际化内容后，同步从${CACHE_NAME}缓存中踢除...")
            val (locale, i18nTypeDictCode, atomicServiceCode) = resolveKeyParts(any, id) ?: return
            CacheKit.evict(CACHE_NAME, getKey(locale, i18nTypeDictCode, atomicServiceCode))
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 批量删除数据库记录后同步缓存
     *
     * @param ids 国际化id集合
     * @param keys 缓存key集合
     */
    open fun syncOnBatchDelete(ids: Collection<String>, keys: Collection<String>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量删除id为${ids}的国际化内容后，同步从${CACHE_NAME}缓存中踢除...")
            keys.forEach { CacheKit.evict(CACHE_NAME, it) }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 返回参数拼接后的缓存key
     */
    fun getKey(locale: String?, i18nTypeDictCode: String?, atomicServiceCode: String?): String {
        return "${locale}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${i18nTypeDictCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${atomicServiceCode}"
    }

    private fun resolveKeyParts(any: Any, id: String): Triple<String, String, String>? {
        val locale = BeanKit.getProperty(any, SysI18n::locale.name) as String?
        val i18nTypeDictCode = BeanKit.getProperty(any, SysI18n::i18nTypeDictCode.name) as String?
        val atomicServiceCode = BeanKit.getProperty(any, SysI18n::atomicServiceCode.name) as String?
        if (locale != null && i18nTypeDictCode != null && atomicServiceCode != null) {
            return Triple(locale, i18nTypeDictCode, atomicServiceCode)
        }
        val i18n = sysI18nDao.get(id)
        if (i18n == null) {
            log.warn("同步国际化缓存时未找到id为${id}的记录。")
            return null
        }
        return Triple(i18n.locale, i18n.i18nTypeDictCode, i18n.atomicServiceCode)
    }

    private val log = LogFactory.getLog(this)

}
