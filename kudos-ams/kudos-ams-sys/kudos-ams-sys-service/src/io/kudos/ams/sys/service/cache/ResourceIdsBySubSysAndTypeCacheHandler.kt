package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ams.sys.service.dao.SysResourceDao
import io.kudos.ams.sys.service.model.po.SysResource
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.Consts
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 资源id（by sub system code & resource type）缓存处理器
 *
 * 1.缓存所有active=true的资源id
 * 2.缓存的key为：subSystemCode::resourceType
 * 3.缓存的value为：资源id
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class ResourceIdsBySubSysAndTypeCacheHandler : AbstractCacheHandler<List<String>>() {

    @Autowired
    private lateinit var sysResourceDao: SysResourceDao

    @Autowired
    private lateinit var self: ResourceIdsBySubSysAndTypeCacheHandler


    companion object {
        private const val CACHE_NAME = "sys_resource_ids_by_sub_sys_and_type"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "缓存${CACHE_NAME}的key格式必须是 子系统代码${Consts.CACHE_KEY_DEFAULT_DELIMITER}资源类型代码"
        }
        val subSysAndResType = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return self.getResourceIds(subSysAndResType[0], subSysAndResType[1])
    }

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有启用状态的资源id！")
            return
        }

        // 加载所有可用的资源
        val criteria = Criteria.add(SysResource::active.name, OperatorEnum.EQ, true)
        val returnProperties =
            listOf(SysResource::id.name, SysResource::subSystemCode.name, SysResource::resourceTypeDictCode.name)

        val results = sysResourceDao.searchProperties(criteria, returnProperties)
        log.debug("从数据库加载了${results.size}条资源信息。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存资源
        val resMap = results.groupBy {
            getKey(
                it[SysResource::subSystemCode.name] as String, it[SysResource::resourceTypeDictCode.name] as String
            )
        }
        resMap.forEach { (key, value) ->
            val v = value.map { it[SysResource::id.name] as String }
            CacheKit.put(CACHE_NAME, key, v)
            log.debug("缓存了key为${key}的${value.size}条资源。")
        }
    }

    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#subSystemCode.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#resourceTypeDictCode)",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getResourceIds(subSystemCode: String, resourceTypeDictCode: String): List<String> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在子系统为${subSystemCode}且资源类型为${resourceTypeDictCode}的资源id，从数据库中加载...")
        }
        require(subSystemCode.isNotBlank()) { "获取资源时，子系统代码必须指定！" }
        require(resourceTypeDictCode.isNotBlank()) { "获取资源时，资源类型代码必须指定！" }
        val criteria = Criteria.add(SysResource::active.name, OperatorEnum.EQ, true)
            .addAnd(SysResource::subSystemCode.name, OperatorEnum.EQ, subSystemCode)
            .addAnd(SysResource::resourceTypeDictCode.name, OperatorEnum.EQ, resourceTypeDictCode)

        @Suppress("UNCHECKED_CAST")
        val ids = sysResourceDao.searchProperty(criteria, SysResource::id.name) as List<String>
        log.debug("从数据库加载了${ids.size}条的资源id。")
        return ids
    }

    open fun syncOnInsert(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("新增id为${id}的资源后，同步${CACHE_NAME}缓存...")
            val subSystemCode = BeanKit.getProperty(any, SysResource::subSystemCode.name) as String
            val resourceTypeDictCode = BeanKit.getProperty(any, SysResource::resourceTypeDictCode.name) as String
            CacheKit.evict(CACHE_NAME, getKey(subSystemCode, resourceTypeDictCode))
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                self.getResourceIds(subSystemCode, resourceTypeDictCode) // 缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    open fun syncOnUpdate(any: Any, id: String, oldsubSystemCode: String, oldResourceTypeDictCode: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的资源后，同步${CACHE_NAME}缓存...")
            val subSystemCode = BeanKit.getProperty(any, SysResource::subSystemCode.name) as String
            val resourceTypeDictCode = BeanKit.getProperty(any, SysResource::resourceTypeDictCode.name) as String
            CacheKit.evict(CACHE_NAME, getKey(oldsubSystemCode, oldResourceTypeDictCode)) // 踢除资源缓存
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                self.getResourceIds(subSystemCode, resourceTypeDictCode) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    open fun syncOnUpdateActive(id: String, active: Boolean) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的资源的启用状态后，同步${CACHE_NAME}缓存...")
            val sysRes = sysResourceDao.get(id)!!
            if (active) {
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    self.getResourceIds(sysRes.subSystemCode, sysRes.resourceTypeDictCode) // 重新缓存
                }
            } else {
                CacheKit.evict(CACHE_NAME, getKey(sysRes.subSystemCode, sysRes.resourceTypeDictCode)) // 踢除资源缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    open fun syncOnDelete(id: String, subSystemCode: String, resourceTypeDictCode: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除id为${id}的资源后，同步从${CACHE_NAME}缓存中踢除...")
            CacheKit.evict(CACHE_NAME, getKey(subSystemCode, resourceTypeDictCode)) // 踢除缓存, 资源缓存的粒度到资源类型
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                self.getResourceIds(subSystemCode, resourceTypeDictCode) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    private fun getKey(subSystemCode: String, resourceTypeDictCode: String): String {
        return "${subSystemCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${resourceTypeDictCode}"
    }

    private val log = LogFactory.getLog(this)


}