package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ams.sys.service.dao.SysResourceDao
import io.kudos.ams.sys.service.model.po.SysResource
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.Consts
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 资源id（by sub system code & url）缓存处理器
 *
 * 1.缓存所有包含url的资源id
 * 2.缓存的key为：subSystemCode::url
 * 3.缓存的value为：资源id
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class ResourceIdBySubSysAndUrlCacheHandler : AbstractCacheHandler<String>() {

    @Autowired
    private lateinit var sysResourceDao: SysResourceDao

    companion object {
        private const val CACHE_NAME = "SYS_RESOURCE_ID_BY_SUB_SYS_AND_URL"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): String? {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "缓存${CACHE_NAME}的key格式必须是 子系统代码${Consts.CACHE_KEY_DEFAULT_DELIMITER}URL"
        }
        val subSysAndUrl = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return getSelf<ResourceIdBySubSysAndUrlCacheHandler>().getResourceId(subSysAndUrl[0], subSysAndUrl[1])
    }

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有包含url的资源id！")
            return
        }

        // 加载所有包含url的资源id
        val criteria = Criteria(Criterion(SysResource::url.name, OperatorEnum.IS_NOT_NULL))

        @Suppress(Consts.Suppress.UNCHECKED_CAST)
        val returnProperties = listOf(SysResource::id.name, SysResource::url.name, SysResource::subSystemCode.name)
        val ids = sysResourceDao.searchProperties(criteria, returnProperties)
        log.debug("从数据库加载了${ids.size}条包含url的资源id。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存资源id
        ids.forEach {
            val key = getKey(it[SysResource::subSystemCode.name] as String, it[SysResource::url.name] as String)
            CacheKit.put(CACHE_NAME, key, it[SysResource::id.name])
        }
        log.debug("缓存了${ids.size}条包含url的资源id。")
    }

    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#subSystemCode.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#url)",
        unless = "#result == null"
    )
    open fun getResourceId(subSystemCode: String, url: String): String? {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在子系统为${subSystemCode}且URL为${url}的资源id，从数据库中加载...")
        }
        require(subSystemCode.isNotBlank()) { "获取资源id时，子系统代码必须指定！" }
        require(url.isNotBlank()) { "获取资源id时，url必须指定！" }
        val criteria = Criteria.add(SysResource::subSystemCode.name, OperatorEnum.EQ, subSystemCode)
            .addAnd(SysResource::url.name, OperatorEnum.EQ, url)
        val ids = sysResourceDao.searchProperty(criteria, SysResource::id.name)
        return if (ids.isEmpty()) {
            log.debug("数据库中不存在子系统为${subSystemCode}且URL为${url}的资源id！")
            null
        } else {
            val id = ids.first() as String
            log.debug("数据库中找到子系统为${subSystemCode}且URL为${url}的资源id：$id！")
            id
        }
    }

    open fun syncOnInsert(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME) && CacheKit.isWriteInTime(CACHE_NAME)) {
            val url = BeanKit.getProperty(any, SysResource::url.name) as String?
            if (!url.isNullOrBlank()) {
                log.debug("新增id为${id}的资源后，同步${CACHE_NAME}缓存...")
                val subSystemCode = BeanKit.getProperty(any, SysResource::subSystemCode.name) as String
                getSelf<ResourceIdBySubSysAndUrlCacheHandler>().getResourceId(subSystemCode, url) // 缓存
                log.debug("${CACHE_NAME}缓存同步完成。")
            }
        }
    }

    open fun syncOnUpdate(any: Any, id: String, oldUrl: String?) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的资源后，同步${CACHE_NAME}缓存...")
            val subSystemCode = BeanKit.getProperty(any, SysResource::subSystemCode.name) as String
            if (!oldUrl.isNullOrBlank()) {
                CacheKit.evict(CACHE_NAME, getKey(subSystemCode, oldUrl))
            }
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                val url = BeanKit.getProperty(any, SysResource::url.name) as String?
                if (!url.isNullOrBlank()) {
                    getSelf<ResourceIdBySubSysAndUrlCacheHandler>().getResourceId(subSystemCode, url) // 重新缓存
                }
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    open fun syncOnDelete(id: String, subSystemCode: String, url: String?) {
        if (!url.isNullOrBlank() && CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除id为${id}的资源后，同步从${CACHE_NAME}缓存中踢除...")
            CacheKit.evict(CACHE_NAME, getKey(subSystemCode, url))
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    private fun getKey(subSystemCode: String, url: String): String {
        return "${subSystemCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${url}"
    }

    private val log = LogFactory.getLog(this)

}