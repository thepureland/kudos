package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ms.sys.common.vo.dict.SysDictRecord
import io.kudos.ms.sys.common.vo.dict.SysDictSearchPayload
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheItem
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemSearchPayload
import io.kudos.ms.sys.core.dao.SysDictDao
import io.kudos.ms.sys.core.dao.SysDictItemDao
import io.kudos.ms.sys.core.model.po.SysDictItem
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 字典项（by module & dict type）缓存处理器
 *
 * 1.数据来源表：sys_dict & sys_dict_item
 * 2.缓存active=true的字典的所有active=true的字典项
 * 3.缓存key为：atomicServiceCode::dictType
 * 4.缓存value为：SysDictItemCacheItem列表，按orderNum排序
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class DictItemsByModuleAndTypeCache : AbstractKeyValueCacheHandler<List<SysDictItemCacheItem>>() {

    @Autowired
    private lateinit var sysDictDao: SysDictDao

    @Autowired
    private lateinit var sysDictItemDao: SysDictItemDao

    @Autowired
    private lateinit var dictByIdCache: DictByIdCache

    companion object {
        private const val CACHE_NAME = "SYS_DICT_ITEMS_BY_MODULE_AND_TYPE"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<SysDictItemCacheItem> {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "缓存${CACHE_NAME}的key格式必须是：模块代码${Consts.CACHE_KEY_DEFAULT_DELIMITER}字典类型代码"
        }
        val moduleAndDictType = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return getSelf<DictItemsByModuleAndTypeCache>().getDictItems(
            moduleAndDictType[0], moduleAndDictType[1]
        )
    }

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有启用状态的字典项！")
            return
        }

        // 加载所有可用的字典项
        val payload = SysDictItemSearchPayload().apply {
            active = true
            dictActive = true
        }
        val results = sysDictItemDao.pagingSearch(payload)

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存数据
        val dictMap = results.groupBy { getKey(it.atomicServiceCode!!, it.dictType!!) }
        dictMap.forEach { (key, value) ->
            val valueItems = value.map { it ->
                SysDictItemCacheItem().apply {
                    id = it.itemId
                    itemCode = it.itemCode
                    itemName = it.itemName
                    parentId = it.parentId
                    orderNum = it.orderNum
                }
            }
            CacheKit.put(CACHE_NAME, key, valueItems)
            log.debug("缓存字典${key}，共${valueItems.size}条字典项。")
        }
    }

    /**
     * 根据原子服务编码和字典类型获取缓存中对应的字典项，如果缓存中不存在，则从数据库加载，并写入缓存
     *
     * @param atomicServiceCode 原子服务编码
     * @param dictType 字典类型
     * @return List<SysDictItemCacheItem>，找不到返回空列表
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#atomicServiceCode.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#dictType)",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getDictItems(atomicServiceCode: String, dictType: String): List<SysDictItemCacheItem> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在模块为${atomicServiceCode}且字典类型为${dictType}的字典项，从数据库中加载...")
        }
        // 查出对应的dict
        val searchPayload = SysDictSearchPayload().apply {
            this.atomicServiceCode = atomicServiceCode
            this.dictType = dictType
            this.active = true
        }
        @Suppress("UNCHECKED_CAST")
        val result = sysDictDao.search(searchPayload) as List<SysDictRecord>

        return if (result.isEmpty()) {
            log.warn("数据库中不存在模块为${atomicServiceCode}且字典类型为${dictType}的active为true字典项！")
            listOf()
        } else {
            // 查出dict id的所有字典项(按orderNum排序)
            val items = sysDictItemDao.searchActiveItemByDictId(result.first().id!!)
            log.debug("数据库中加载到模块为${atomicServiceCode}且字典类型为${dictType}的字典项共${items.size}条.")
            items.map {
                SysDictItemCacheItem().apply {
                    id = it.id
                    itemCode = it.itemCode
                    itemName = it.itemName
                    parentId = it.parentId
                    orderNum = it.orderNum
                }
            }
        }
    }

    /**
     * 数据库插入记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 字典项id
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("新增id为${id}的字典项后，同步${CACHE_NAME}缓存...")
            val dictId = BeanKit.getProperty(any, SysDictItem::dictId.name) as String
            val dict = dictByIdCache.getDictById(dictId)
            if (dict == null) {
                log.error("缓存${dictByIdCache.cacheName()}中找不到id为${id}的字典！")
                return
            }

            val key = getKey(dict.atomicServiceCode, dict.dictType)
            CacheKit.evict(CACHE_NAME, key) // 踢除缓存（缓存粒度为字典类型）
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                if (dict.active == null || dict.active == true) {
                    getSelf<DictItemsByModuleAndTypeCache>().getDictItems(
                        dict.atomicServiceCode!!, dict.dictType!!
                    )
                    log.debug("${CACHE_NAME}缓存同步完成。")
                } else {
                    log.debug("新增的字典项的字典active为false，不需要同步回写${CACHE_NAME}缓存。")
                }
            }

        }
    }

    /**
     * 更新数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 字典项id
     */
    open fun syncOnUpdate(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的字典项后，同步${CACHE_NAME}缓存...")
            val dictId = BeanKit.getProperty(any, SysDictItem::dictId.name) as String
            val dict = dictByIdCache.getDictById(dictId)
            if (dict == null) {
                log.error("缓存${dictByIdCache.cacheName()}中找不到id为${id}的字典！")
                return
            }

            val key = getKey(dict.atomicServiceCode, dict.dictType)
            CacheKit.evict(CACHE_NAME, key) // 踢除缓存（缓存粒度为字典类型）
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                if (dict.active == null || dict.active == true) {
                    getSelf<DictItemsByModuleAndTypeCache>().getDictItems(
                        dict.atomicServiceCode!!, dict.dictType!!
                    )
                    log.debug("${CACHE_NAME}缓存同步完成。")
                } else {
                    log.debug("更新的字典项的字典active为false，不需要同步回写${CACHE_NAME}缓存。")
                }
            }
        }
    }

    /**
     * 更新启用状态后同步缓存
     *
     * @param id 字典项id
     */
    open fun syncOnUpdateActive(id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的字典项的启用状态后，同步${CACHE_NAME}缓存...")
            val dictIds = sysDictItemDao.oneSearchProperty(SysDictItem::id.name, id, SysDictItem::dictId.name)
            val dict = dictByIdCache.getDictById(dictIds.first() as String)
            if (dict == null) {
                log.error("缓存${dictByIdCache.cacheName()}中找不到id为${id}的字典！")
                return
            }

            CacheKit.evict(CACHE_NAME, getKey(dict.atomicServiceCode, dict.dictType)) // 踢除缓存（缓存粒度为字典类型）
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                // 重新缓存
                getSelf<DictItemsByModuleAndTypeCache>().getDictItems(dict.atomicServiceCode!!, dict.dictType!!)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 删除数据库记录后同步缓存
     *
     * @param id 字典项id
     * @param dictId 字典id
     */
    open fun syncOnDelete(id: String, dictId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除id为${id}的租户后，同步从${CACHE_NAME}缓存中踢除...")
            val dict = dictByIdCache.getDictById(dictId)
            if (dict == null) {
                log.error("缓存${dictByIdCache.cacheName()}中找不到id为${id}的字典！")
                return
            }

            CacheKit.evict(CACHE_NAME, getKey(dict.atomicServiceCode, dict.dictType)) // 踢除缓存（缓存粒度为字典类型）
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                // 重新缓存
                getSelf<DictItemsByModuleAndTypeCache>().getDictItems(dict.atomicServiceCode!!, dict.dictType!!)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 返回参数拼接后的缓存的key
     *
     * @param atomicServiceCode 原子服务编码
     * @param dictType 字典类型
     * @return 缓存的key
     */
    fun getKey(atomicServiceCode: String?, dictType: String?): String {
        return "${atomicServiceCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${dictType}"
    }

    private val log = LogFactory.getLog(this)

}