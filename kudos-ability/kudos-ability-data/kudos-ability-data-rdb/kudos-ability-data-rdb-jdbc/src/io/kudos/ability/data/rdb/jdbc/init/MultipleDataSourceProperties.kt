package io.kudos.ability.data.rdb.jdbc.init

import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.InitializingBean
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.Volatile

/**
 * "包路径 → 数据源 key"的配置属性 bean，对应 yml 中的 `kudos.ability.jdbc.*`。
 *
 * 工作机制：
 *  - [packageDataSource]：yml 配置的 (前缀包名, 数据源 key) 映射，可热更新（[forceChangeDataSource]）
 *  - [serviceDataSource]：运行时缓存"具体 service 类的包名 → 数据源 key"的解析结果
 *
 * 第一次查询某个具体类的包路径时，按 [packageDataSource] 的前缀匹配规则解析一次并缓存；
 * 热更新会清空整个缓存让下次查询重新解析。
 *
 * 已知问题：[lookDataSourceKey] 同时用 `ConcurrentHashMap.computeIfAbsent` + readLock，
 * 前者已经并发安全，后者只是给 [forceChangeDataSource] 的 writeLock 做"对仗"，实际上保护
 * 不了什么（参见 README 限制列表）。
 *
 * @author hanson
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class MultipleDataSourceProperties : InitializingBean {
    private val log = LogFactory.getLog(this::class)

    /** yml 配置的"包前缀 → 数据源 key"映射；可被 [forceChangeDataSource] 热更新。 */
    @Volatile
    var packageDataSource = ConcurrentHashMap<String, String>()
    private val serviceDataSource = ConcurrentHashMap<String, String>()
    private val rw: ReadWriteLock = ReentrantReadWriteLock()

    /**
     * 按完整包名取数据源 key（不做前缀匹配，要求精确匹配 [packageDataSource] 的 key）。
     */
    fun lookUpKey(key: String?): String? {
        if (packageDataSource.containsKey(key)) {
            return packageDataSource.get(key)
        }
        return null
    }

    /**
     * 按 service 类的包名查数据源 key：先看 [serviceDataSource] 缓存，没有则按
     * [packageDataSource] 的前缀匹配解析后缓存。返回 `""` 表示"没有匹配的数据源配置"。
     */
    fun lookDataSourceKey(serviceClazz: Class<*>): String {
        val packageName = serviceClazz.getPackageName()
        rw.readLock().lock()
        try {
            var result = serviceDataSource.get(packageName)
            if (result.isNullOrBlank()) {
                result = serviceDataSource.computeIfAbsent(packageName) { _ ->
                    for (entry in packageDataSource.entries) {
                        if (packageName.startsWith(entry.key)) {
                            return@computeIfAbsent entry.value
                        }
                    }
                    ""
                }
            }
            return result
        } finally {
            rw.readLock().unlock()
        }
    }

    /**
     * 业务代码运行时强制修改某个包路径的数据源；同时清空 [serviceDataSource] 缓存让下次
     * 查询重新解析。线程安全（writeLock 保护）。
     */
    fun forceChangeDataSource(packageName: String, dataSource: String) {
        packageDataSource[packageName] = dataSource
        rw.writeLock().lock()
        try {
            serviceDataSource.clear()
        } finally {
            rw.writeLock().unlock()
        }
        log.info("强制变更{0}，数据源为:{1}", packageName, dataSource)
    }

    /** Spring [InitializingBean] 回调：bean 装配完成后打印当前数据源配置一览（DEBUG 级）。 */
    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        log.debug("Begin.....输出数据源设置信息....Begin")
        for (entry in packageDataSource.entries) {
            log.debug("{0}={1}", entry.key, entry.value)
        }
        log.debug("End.....输出数据源设置信息....End")
    }
}
