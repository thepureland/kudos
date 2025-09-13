package io.kudos.ability.data.rdb.jdbc.init

import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.InitializingBean
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.Volatile

/**
 * 数据源配置
 *
 * @author hanson
 * @author K
 * @since 1.0.0
 */
class MultipleDataSourceProperties : InitializingBean {
    private val log = LogFactory.getLog(this)

    /**
     * 获取所有动态数据源配置
     */
    @Volatile
    var packageDataSource: MutableMap<String?, String?> = ConcurrentHashMap<String?, String?>()
    private val serviceDataSource: MutableMap<String?, String?> = ConcurrentHashMap<String?, String?>()
    private val rw: ReadWriteLock = ReentrantReadWriteLock()

    /**
     * 获取组件所使用的数据源
     *
     * @param key
     */
    fun lookUpKey(key: String?): String? {
        if (packageDataSource.containsKey(key)) {
            return packageDataSource.get(key)
        }
        return null
    }

    /**
     * 根据切面获取数据源ID
     *
     * @param serviceClazz
     */
    fun lookDataSourceKey(serviceClazz: Class<*>): String? {
        val packageName = serviceClazz.getPackageName()
        rw.readLock().lock()
        try {
            var result = serviceDataSource.get(packageName)
            if (result.isNullOrBlank()) {
                result = serviceDataSource.computeIfAbsent(packageName) { k: String? ->
                    for (entry in packageDataSource.entries) {
                        if (packageName.startsWith(entry.key!!)) {
                            return@computeIfAbsent entry.value
                        }
                    }
                    null
                }
            }
            return result
        } finally {
            rw.readLock().unlock()
        }
    }

    /**
     * 提供业务根据编码方式，指定某个路径的数据源
     *
     * @param packageName
     * @param dataSource
     */
    fun forceChangeDataSource(packageName: String?, dataSource: String?) {
        packageDataSource.put(packageName, dataSource)
        rw.writeLock().lock()
        try {
            serviceDataSource.clear()
        } finally {
            rw.writeLock().unlock()
        }
        log.info("强制变更{0}，数据源为:{1}", packageName, dataSource)
    }

    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        log.debug("Begin.....输出数据源设置信息....Begin")
        for (entry in packageDataSource.entries) {
            log.debug("{0}={1}", entry.key, entry.value)
        }
        log.debug("End.....输出数据源设置信息....End")
    }
}
