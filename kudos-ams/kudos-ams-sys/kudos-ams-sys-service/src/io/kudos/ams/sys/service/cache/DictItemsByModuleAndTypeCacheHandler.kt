package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ams.sys.common.vo.dict.SysDictSearchPayload
import io.kudos.ams.sys.common.vo.dictitem.SysDictItemCacheItem
import io.kudos.ams.sys.service.dao.SysDictDao
import io.kudos.ams.sys.service.dao.SysDictItemDao
import io.kudos.ams.sys.service.model.po.SysDictItem
import io.kudos.base.logger.LogFactory
import io.kudos.base.support.Consts
import io.kudos.context.kit.SpringKit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 字典项（by module & dict type）缓存处理器
 *
 * 1. 缓存所有active=true的字典项
 * 2. 缓存key为：moduleCode::dictType
 * 3. 缓存value为：SysDictItemCacheItem对象
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class DictItemsByModuleAndTypeCacheHandler : AbstractCacheHandler<List<SysDictItemCacheItem>>() {

    @Autowired
    private lateinit var sysDictDao: SysDictDao

    @Autowired
    private lateinit var sysDictItemDao: SysDictItemDao

    @Autowired
    private lateinit var dictCacheHandler: DictByIdCacheHandler

    private var self: DictItemsByModuleAndTypeCacheHandler? = null

    companion object {
        private const val CACHE_NAME = "SYS_DICT_ITEMS_BY_MODULE_AND_TYPE"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<SysDictItemCacheItem> {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "缓存${CACHE_NAME}的key格式必须是 模块代码${Consts.CACHE_KEY_DEFAULT_DELIMITER}字典类型代码"
        }
        val moduleAndDictType = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return getSelf().getItemsFromCache(moduleAndDictType[0], moduleAndDictType[1])
    }

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有启用状态的字典项！")
            return
        }

        // 加载所有可用的字典
        val payload = SysDictSearchPayload().apply {
            active = true
        }
        val results = sysDictDao.pagingSearch(payload)

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存数据
        val dictMap = results.groupBy { getKey(it.moduleCode!!, it.dictType!!) }
        dictMap.forEach { (key, value) ->
            val valueItems = value.map {
                SysDictItemCacheItem().apply {
                    itemCode = it.itemCode
                    itemName = it.itemName
                    parentId = it.parentId
                    seqNo = it.seqNo
                }
            }
            CacheKit.putIfAbsent(CACHE_NAME, key, valueItems)
            log.debug("缓存字典${key}，共${valueItems.size}条字典项。")
        }
    }

    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#module.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#type)",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getItemsFromCache(module: String, type: String): List<SysDictItemCacheItem> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在模块为${module}且字典类型为${type}的字典项，从数据库中加载...")
        }
        // 查出对应的dict id
        val dictId = sysDictDao.getDictIdByModuleAndType(module, type)

        return if (dictId == null) {
            log.warn("数据库中不存在模块为${module}且字典类型为${type}的字典项！")
            listOf()
        } else {
            // 查出dict id的所有字典项
            val items = sysDictItemDao.searchByDictId(dictId)
            log.debug("数据库中加载到模块为${module}且字典类型为${type}的字典项共${items.size}条.")
            items.map {
                SysDictItemCacheItem().apply {
                    itemCode = it.itemCode
                    itemName = it.itemName
                    parentId = it.parentId
                    seqNo = it.orderNum
                }
            }
        }
    }

    open fun syncOnInsert(sysDictItem: SysDictItem) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("新增id为${sysDictItem.id}的字典项后，同步${CACHE_NAME}缓存...")
            val dict = dictCacheHandler.getDictById(sysDictItem.dictId)!!
            CacheKit.evict(CACHE_NAME, "${dict.module}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${dict.dictType}") // 踢除缓存
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf().getItemsFromCache(dict.module!!, dict.dictType!!)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    open fun syncOnUpdate(sysDictItem: SysDictItem) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${sysDictItem.id}的字典项后，同步${CACHE_NAME}缓存...")
            val dict = dictCacheHandler.getDictById(sysDictItem.dictId)!!
            CacheKit.evict(CACHE_NAME, "${dict.module}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${dict.dictType}") // 踢除缓存
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf().getItemsFromCache(dict.module!!, dict.dictType!!)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    open fun syncOnUpdateActive(dictItemId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${dictItemId}的字典项的启用状态后，同步${CACHE_NAME}缓存...")
            val dictIds = sysDictItemDao.oneSearchProperty(SysDictItem::id.name, dictItemId, SysDictItem::dictId.name)
            val dict = sysDictDao.get(dictIds.first() as String)!!
            CacheKit.evict(
                CACHE_NAME, getKey(dict.moduleCode, dict.dictType)
            ) // 字典的缓存粒度为字典类型
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf().getItemsFromCache(dict.moduleCode, dict.dictType) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    open fun syncOnDelete(id: String, dictId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除id为${id}的租户后，同步从${CACHE_NAME}缓存中踢除...")
            val dict = sysDictDao.get(dictId)!!
            CacheKit.evict(CACHE_NAME, getKey(dict.moduleCode, dict.dictType)) // 字典的缓存粒度为字典类型
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf().getItemsFromCache(dict.moduleCode, dict.dictType) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    private fun getKey(module: String, dictType: String): String {
        return "${module}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${dictType}"
    }

    private fun getSelf() : DictItemsByModuleAndTypeCacheHandler {
        if (self == null) {
            self = SpringKit.getBean(this::class)
        }
        return self!!
    }

    private val log = LogFactory.getLog(this)

}