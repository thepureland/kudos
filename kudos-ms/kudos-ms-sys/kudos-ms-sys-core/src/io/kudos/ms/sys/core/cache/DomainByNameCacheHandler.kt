package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ms.sys.common.vo.domain.SysDomainCacheItem
import io.kudos.ms.sys.common.vo.domain.SysDomainSearchPayload
import io.kudos.ms.sys.core.dao.SysDomainDao
import io.kudos.ms.sys.core.model.po.SysDomain
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 域名缓存处理器
 *
 * 1.数据来源表：sys_domain
 * 2.缓存域名，不包含active=false的
 * 3.缓存的key为：domain name
 * 4.缓存的value为：SysDomainCacheItem对象
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class DomainByNameCacheHandler : AbstractCacheHandler<SysDomainCacheItem>() {

    @Autowired
    private lateinit var dao: SysDomainDao

    companion object {
        private const val CACHE_NAME = "SYS_DOMAIN_BY_NAME"
    }

    override fun cacheName() = CACHE_NAME

    override fun doReload(key: String): SysDomainCacheItem? {
        return getSelf<DomainByNameCacheHandler>().getDomain(key)
    }

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有域名信息！")
            return
        }

        // 加载所有active为true的域名
        val searchPayload = SysDomainSearchPayload().apply {
            returnEntityClass = SysDomainCacheItem::class
            active = true
        }

        @Suppress("UNCHECKED_CAST")
        val domains = dao.search(searchPayload) as List<SysDomainCacheItem>
        log.debug("从数据库加载了${domains.size}条域名信息。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存域名
        domains.forEach {
            CacheKit.put(CACHE_NAME, it.domain!!, it)
        }
        log.debug("缓存了${domains.size}条域名信息。")
    }

    /**
     * 根据名称从缓存中获取域名信息，如果缓存中不存在，则从数据库中加载，并写入缓存
     *
     * @param domain 域名名称
     * @return SysDomainCacheItem对象，如果找不到返回null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#domain",
        unless = "#result == null"
    )
    open fun getDomain(domain: String): SysDomainCacheItem? {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在域名${domain}，从数据库中加载...")
        }

        val searchPayload = SysDomainSearchPayload().apply {
            returnEntityClass = SysDomainCacheItem::class
            this.domain = domain
            active = true
        }

        @Suppress("UNCHECKED_CAST")
        val domains = dao.search(searchPayload) as List<SysDomainCacheItem>
        return if (domains.isEmpty()) {
            log.debug("从数据库找不到active=true且名为${domain}的域名信息。")
            null
        } else {
            log.debug("从数据库加载了名为${domain}的域名信息。")
            domains.first()
        }
    }

    /**
     * 数据库插入记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 域名id
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME) && CacheKit.isWriteInTime(CACHE_NAME)) {
            log.debug("新增id为${id}的域名后，同步${CACHE_NAME}缓存...")
            val domain = BeanKit.getProperty(any, SysDomain::domain.name) as String
            CacheKit.evict(CACHE_NAME, domain) // 踢除缓存
            getSelf<DomainByNameCacheHandler>().getDomain(domain) // 重新缓存
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 更新数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 域名id
     */
    open fun syncOnUpdate(any: Any?, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的域名后，同步${CACHE_NAME}缓存...")
            val domain = if (any == null) {
                dao.get(id)!!.domain
            } else {
                BeanKit.getProperty(any, SysDomain::domain.name) as String
            }
            CacheKit.evict(CACHE_NAME, domain) // 踢除缓存
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<DomainByNameCacheHandler>().getDomain(domain) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 删除数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 域名id
     */
    open fun syncOnDelete(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除id为${id}的域名后，同步从${CACHE_NAME}缓存中踢除...")
            val domain = BeanKit.getProperty(any, SysDomain::domain.name) as String
            CacheKit.evict(CACHE_NAME, domain) // 踢除缓存
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 批量删除数据库记录后同步缓存
     *
     * @param ids 域名id集合
     * @param domains 域名名称集合
     */
    open fun syncOnBatchDelete(ids: Collection<String>, domains: Set<String>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量删除id为${ids}的域名后，同步从${CACHE_NAME}缓存中踢除...")
            domains.forEach {
                CacheKit.evict(CACHE_NAME, it) // 踢除缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    private val log = LogFactory.getLog(this)

}