package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ams.sys.common.vo.param.SysParamCacheItem
import io.kudos.ams.sys.common.vo.param.SysParamSearchPayload
import io.kudos.ams.sys.service.dao.SysParamDao
import io.kudos.ams.sys.service.model.po.SysParam
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 参数（by module & name）缓存处理器
 *
 * 1.数据来源表：sys_param
 * 2.缓存所有active=true的参数
 * 3.缓存的key为：moduleCode::paramName
 * 4.缓存的value为：SysParamCacheItem对象
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class ParamByModuleAndNameCacheHandler : AbstractCacheHandler<SysParamCacheItem>() {

    @Autowired
    private lateinit var sysParamDao: SysParamDao

    companion object {
        private const val CACHE_NAME = "SYS_PARAM_BY_MODULE_AND_NAME"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): SysParamCacheItem? {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "缓存${CACHE_NAME}的key格式必须是 模块代码${Consts.CACHE_KEY_DEFAULT_DELIMITER}参数名称"
        }
        val moduleAndParamName = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return getSelf<ParamByModuleAndNameCacheHandler>().getParam(
            moduleAndParamName[0], moduleAndParamName[1]
        )
    }

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有启用状态的参数！")
            return
        }

        // 加载所有可用的参数
        val searchPayload = SysParamSearchPayload().apply {
            active = true
            returnEntityClass = SysParamCacheItem::class
        }

        @Suppress("UNCHECKED_CAST")
        val params = sysParamDao.search(searchPayload) as List<SysParamCacheItem>
        log.debug("从数据库加载了${params.size}条参数信息。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存参数
        params.forEach {
            CacheKit.put(CACHE_NAME, getKey(it.moduleCode!!, it.paramName!!), it)
        }
        log.debug("缓存了${params.size}条参数信息。")
    }

    /**
     * 根据模块编号和参数名称从缓存获取对应的参数信息，如果缓存中不存在，则从数据库中加载，并写回缓存
     *
     * @param moduleCode 模块编号
     * @param paramName 参数名称
     * @return SysParamCacheItem，找不到返回null
     */
    @Cacheable(
        value = [CACHE_NAME],
        key = "#moduleCode.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#paramName)",
        unless = "#result == null"
    )
    open fun getParam(moduleCode: String, paramName: String): SysParamCacheItem? {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在模块为${moduleCode}且名称为${paramName}的参数，从数据库中加载...")
        }
        val param = sysParamDao.getActiveParamsForCache(moduleCode, paramName)
        if (param == null) {
            log.warn("数据库中不存在模块为${moduleCode}且名称为${paramName}的active=true的参数！")
        }
        return param
    }

    /**
     * 数据库插入记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 参数id
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME) && CacheKit.isWriteInTime(CACHE_NAME)) {
            log.debug("新增id为${id}的参数后，同步${CACHE_NAME}缓存...")
            val moduleCode = BeanKit.getProperty(any, SysParam::moduleCode.name) as String
            val paramName = BeanKit.getProperty(any, SysParam::paramName.name) as String
            getSelf<ParamByModuleAndNameCacheHandler>().getParam(moduleCode, paramName) // 缓存
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 更新数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 参数id
     */
    open fun syncOnUpdate(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的参数后，同步${CACHE_NAME}缓存...")
            val moduleCode = BeanKit.getProperty(any, SysParam::moduleCode.name) as String
            val paramName = BeanKit.getProperty(any, SysParam::paramName.name) as String
            CacheKit.evict(CACHE_NAME, getKey(moduleCode, paramName)) // 踢除参数缓存
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<ParamByModuleAndNameCacheHandler>().getParam(moduleCode, paramName) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 更新启用状态后同步缓存
     *
     * @param id 参数id
     * @param active 是否启用
     */
    open fun syncOnUpdateActive(id: String, active: Boolean) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的参数的启用状态后，同步缓存...")
            val sysParam = sysParamDao.get(id)!!
            if (active) {
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<ParamByModuleAndNameCacheHandler>().getParam(
                        sysParam.moduleCode, sysParam.paramName
                    )
                }
            } else {
                CacheKit.evict(CACHE_NAME, getKey(sysParam.moduleCode, sysParam.paramName)) // 踢除参数缓存
            }
            log.debug("缓存同步完成。")
        }
    }

    /**
     * 删除数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 参数id
     */
    open fun syncOnDelete(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            val moduleCode = BeanKit.getProperty(any, SysParam::moduleCode.name) as String
            val paramName = BeanKit.getProperty(any, SysParam::paramName.name) as String
            log.debug("删除id为${id}的参数后，同步从${CACHE_NAME}缓存中踢除...")
            CacheKit.evict(CACHE_NAME, getKey(moduleCode, paramName)) // 踢除缓存
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 批量删除数据库记录后同步缓存
     *
     * @param ids 参数id集合
     * @param moduleAndNames List<Pair<模块编码，参数名称>>
     */
    open fun syncOnBatchDelete(ids: Collection<String>, moduleAndNames: List<Pair<String, String>>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量删除id为${ids}的参数后，同步从${CACHE_NAME}缓存中踢除...")
            moduleAndNames.forEach {
                CacheKit.evict(CACHE_NAME, getKey(it.first, it.second)) // 踢除缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 返回参数拼接后的key
     *
     * @param moduleCode 模块编码
     * @param paramName 参数名称
     * @return 缓存key
     */
    fun getKey(moduleCode: String, paramName: String): String {
        return "${moduleCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${paramName}"
    }

    private val log = LogFactory.getLog(this)

}