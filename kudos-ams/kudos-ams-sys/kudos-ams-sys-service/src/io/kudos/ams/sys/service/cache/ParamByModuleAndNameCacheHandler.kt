package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ability.data.rdb.jdbc.kit.RdbKit
import io.kudos.ability.data.rdb.ktorm.kit.getDatabase
import io.kudos.ams.sys.common.vo.param.SysParamCacheItem
import io.kudos.ams.sys.common.vo.param.SysParamSearchPayload
import io.kudos.ams.sys.service.dao.SysParamDao
import io.kudos.ams.sys.service.model.po.SysParam
import io.kudos.ams.sys.service.model.table.SysParams
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.support.Consts
import org.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 参数（by module & name）缓存处理器
 *
 * 1.缓存所有active=true的参数
 * 2.缓存的key为：moduleCode::paramName
 * 3.缓存的value为：SysParamCacheItem对象
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
        return getSelf<ParamByModuleAndNameCacheHandler>().getParamFromCache(
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
            CacheKit.putIfAbsent(CACHE_NAME, getKey(it.moduleCode!!, it.paramName!!), it)
        }
        log.debug("缓存了${params.size}条参数信息。")
    }

    @Cacheable(
        value = [CACHE_NAME],
        key = "#module.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#name)",
        unless = "#result == null"
    )
    open fun getParamFromCache(module: String, name: String): SysParamCacheItem? {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在模块为${module}且名称为${name}的参数，从数据库中加载...")
        }
        val paramList = RdbKit.getDatabase().from(SysParams)
            .select(SysParams.columns)
            .whereWithConditions {
                it += (SysParams.paramName eq name) and (SysParams.active eq true)
                if (module.isNotEmpty()) {
                    it += SysParams.moduleCode eq module
                }
            }
            .map { row ->
                val entity = SysParams.createEntity(row)
                BeanKit.copyProperties(entity, SysParamCacheItem())
            }
            .toList()
        return if (paramList.isEmpty()) {
            log.warn("数据库中不存在模块为${module}且名称为${name}的参数！")
            null
        } else {
            log.debug("数据库中加载到模块为${module}且名称为${name}的参数。")
            paramList.first()
        }
    }

    open fun syncOnInsert(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME) && CacheKit.isWriteInTime(CACHE_NAME)) {
            log.debug("新增id为${id}的参数后，同步${CACHE_NAME}缓存...")
            val module = BeanKit.getProperty(any, SysParam::moduleCode.name) as String
            val paramName = BeanKit.getProperty(any, SysParam::paramName.name) as String
            getSelf<ParamByModuleAndNameCacheHandler>().getParamFromCache(module, paramName) // 缓存
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    open fun syncOnUpdate(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的参数后，同步${CACHE_NAME}缓存...")
            val module = BeanKit.getProperty(any, SysParam::moduleCode.name) as String
            val paramName = BeanKit.getProperty(any, SysParam::paramName.name) as String
            CacheKit.evict(CACHE_NAME, getKey(module, paramName)) // 踢除参数缓存
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<ParamByModuleAndNameCacheHandler>().getParamFromCache(module, paramName) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    open fun syncOnUpdateActive(id: String, active: Boolean) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的参数的启用状态后，同步缓存...")
            val sysParam = sysParamDao.get(id)!!
            if (active) {
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<ParamByModuleAndNameCacheHandler>().getParamFromCache(
                        sysParam.moduleCode, sysParam.paramName
                    )
                }
            } else {
                CacheKit.evict(CACHE_NAME, getKey(sysParam.moduleCode, sysParam.paramName)) // 踢除参数缓存
            }
            log.debug("缓存同步完成。")
        }
    }

    open fun syncOnDelete(param: SysParam) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除id为${param.id}的参数后，同步从${CACHE_NAME}缓存中踢除...")
            CacheKit.evict(CACHE_NAME, getKey(param.moduleCode, param.paramName)) // 踢除缓存
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    open fun syncOnBatchDelete(ids: Collection<String>, params: List<SysParam>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量删除id为${ids}的参数后，同步从${CACHE_NAME}缓存中踢除...")
            params.forEach {
                CacheKit.evict(CACHE_NAME, getKey(it.moduleCode, it.paramName)) // 踢除缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    private fun getKey(module: String, paramName: String): String {
        return "${module}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${paramName}"
    }

    private val log = LogFactory.getLog(this)

}