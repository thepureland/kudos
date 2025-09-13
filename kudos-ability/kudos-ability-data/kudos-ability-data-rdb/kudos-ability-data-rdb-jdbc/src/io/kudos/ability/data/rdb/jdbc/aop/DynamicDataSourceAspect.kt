package io.kudos.ability.data.rdb.jdbc.aop

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder
import io.kudos.ability.data.rdb.jdbc.consts.DatasourceConst
import io.kudos.ability.data.rdb.jdbc.context.DbContext
import io.kudos.ability.data.rdb.jdbc.datasource.DsContextProcessor
import io.kudos.ability.data.rdb.jdbc.init.MultipleDataSourceProperties
import io.kudos.ability.data.rdb.jdbc.kit.DatasourceKeyTool
import io.kudos.base.logger.LogFactory
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.soul.context.core.CommonContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.Order
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * 动态数据源切面
 *
 * @author hanson
 * @author K
 * @since 1.0.0
 */
@Aspect
@Order(-99)
class DynamicDataSourceAspect {
    @Autowired
    private lateinit var dataSourceProperties: MultipleDataSourceProperties

    @Autowired
    private lateinit var dsContextProcessor: DsContextProcessor

    /**
     * 拦截所有service包路径下的service
     */
    @Pointcut("within(*..service..*)")
    fun aspService() {
    }

    @Around("aspService()")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val changeDatasource = changeDatasource(joinPoint.target.javaClass)
        try {
            return joinPoint.proceed()
        } catch (e: Exception) {
            throw e
        } catch (e: Throwable) {
            throw RuntimeException("方法执行报错！", e)
        } finally {
            if (changeDatasource) {
                DynamicDataSourceContextHolder.poll()
                log.debug("回退数据源.joinPoint.target.javaClass.getPackageName()")
            }
        }
    }

    private fun changeDatasource(serviceClazz: Class<*>): Boolean {
        if (forceChangeMain()) {
            return true
        }
        //找不到包所对应的数据源配置，则适用默认
        val dsKeyConfig: String? = dataSourceProperties.lookDataSourceKey(serviceClazz)
        if (dsKeyConfig.isNullOrBlank()) {
            return false
        }
        //约定 指定_context则为数据源表，_context无需配置数据源，自动会从datasource里获取
        if (dsKeyConfig!!.startsWith(CONTEXT_DATASOURCE)) {
            val datasourcePair = convertDatasourceConfig(dsKeyConfig)
            //兼容多租户，不同上下文的数据源不同
            val mapKey: String? = DatasourceKeyTool.convertCacheMapKey(
                datasourcePair.first!!,
                CommonContext.get()._datasourceTenantId(), datasourcePair.second
            )
            //如果已经切换过了数据源，则缓存起来
            var dsKey: String? = dsCacheMap.get(mapKey)
            if (dsKey.isNullOrBlank()) {
                READ_WRITE_LOCK.readLock().lock()
                try {
                    dsKey = dsCacheMap.computeIfAbsent(mapKey) { k: String? ->
                        dsContextProcessor.doDetermineDatasource(k!!, dsKeyConfig)
                    }
                } finally {
                    READ_WRITE_LOCK.readLock().unlock()
                }
            }
            DynamicDataSourceContextHolder.push(dsKey)
            log.debug("动态切换数据源,{0}={1}", serviceClazz.getPackageName(), dsKey)
        } else {
            DynamicDataSourceContextHolder.push(dsKeyConfig)
            log.debug("动态切换数据源,{0}={1}", serviceClazz.getPackageName(), dsKeyConfig)
        }
        return true
    }

    /**
     * 强制指定yml文件配置的数据源
     *
     * @return
     */
    private fun forceChangeMain(): Boolean {
        val forcedDs: String? = DbContext.get().forcedDs
        if (forcedDs.isNullOrBlank()) {
            return false
        }
        if (DbContext.get().readonly || forcedDs!!.startsWith(CONTEXT_DATASOURCE)) {
            return false
        }
        if (!dsContextProcessor.haveDataSource(forcedDs)) {
            return false
        }
        //适配：DsChange
        DynamicDataSourceContextHolder.push(forcedDs)
        if (DbContext.get().enableLog) {
            log.info("强制指定数据源：{0}", forcedDs)
        }
        return true
    }


    private fun convertDatasourceConfig(dsKeyConfig: String?): Pair<String?, String?> {
        val mapKeySuffix = DatasourceConst.MODE_MASTER
        val forceDs = DbContext.get().forcedDs
        //动态数据源才需要去判断强制切换
        if (!DbContext.get().forcedDs.isNullOrBlank()) {
            return if (DbContext.get().readonly == true) {
                //只读库设置
                Pair(dsKeyConfig, DatasourceConst.MODE_READONLY)
            } else {
                //如果不是readOnly，则将forceDs当作数据源key。因为非context开头的在第一步就被匹配走了
                //适配：TenantDsChange
                Pair(forceDs, mapKeySuffix)
            }
        }
        //没有强制指定数据源，则返回默认主库
        return Pair(dsKeyConfig, DatasourceConst.MODE_MASTER)
    }

    companion object {
        private const val CONTEXT_DATASOURCE = "_context"
        private val log= LogFactory.getLog(this)
        private val dsCacheMap: MutableMap<String?, String?> = ConcurrentHashMap<String?, String?>()

        private val READ_WRITE_LOCK: ReadWriteLock = ReentrantReadWriteLock()

        @Synchronized
        fun cacheDsCache() {
            READ_WRITE_LOCK.writeLock().lock()
            try {
                dsCacheMap.clear()
            } finally {
                READ_WRITE_LOCK.writeLock().unlock()
            }
        }
    }
}
