package io.kudos.ability.data.rdb.jdbc.aop

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder
import io.kudos.ability.data.rdb.jdbc.consts.DatasourceConst
import io.kudos.ability.data.rdb.jdbc.context.DbContext
import io.kudos.ability.data.rdb.jdbc.datasource.DsContextProcessor
import io.kudos.ability.data.rdb.jdbc.init.MultipleDataSourceProperties
import io.kudos.ability.data.rdb.jdbc.kit.DatasourceKeyTool
import io.kudos.base.logger.LogFactory
import io.kudos.context.core.KudosContextHolder
import jakarta.annotation.Resource
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.springframework.core.annotation.Order
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * 动态数据源路由切面：拦截 `*..biz..*` 包路径下所有方法，根据 [DbContext] 里的
 * forcedDs / [KudosContextHolder] 的租户上下文 / [MultipleDataSourceProperties]
 * 配置的包路径映射，决定本次方法走哪个数据源。
 *
 * 决策优先级：
 *  1. `DbContext.forcedDs` 非空且不是只读、不是 `_context` 前缀 → 直接切到 forcedDs
 *  2. 切面命中的 service 类的包路径在 `dataSourceProperties.lookDataSourceKey` 里有配置
 *     a. 配置以 `_context` 开头 → 走"租户 + 服务 + 模式"动态解析（见 [DsContextProcessor]）
 *     b. 否则配置即数据源 key
 *  3. 都不匹配 → 不切换，沿用上层调用栈已经设的数据源
 *
 * 已知限制：pointcut `within(*..biz..*)` 写死要求业务代码必须在 `biz` 子包下；非该
 * 结构的项目此切面不会生效（不可配置）。
 *
 * @author hanson
 * @author K
 * @since 1.0.0
 */
@Aspect
@Order(-99)
class DynamicDataSourceAspect {

    @Resource
    private lateinit var dataSourceProperties: MultipleDataSourceProperties

    @Resource
    private lateinit var dsContextProcessor: DsContextProcessor

    /**
     * pointcut：所有 `*..biz..*` 包路径下的类的方法。
     */
    @Pointcut("within(*..biz..*)")
    fun aspService() {
    }

    /**
     * 环绕通知。根据上下文计算是否需要 push 数据源到 baomidou 的
     * [DynamicDataSourceContextHolder]，proceed 后 finally 阶段 pop 回去；不抛额外异常，
     * 原始异常直接向上传播。
     */
    @Around("aspService()")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val changeDatasource = changeDatasource(joinPoint.target.javaClass)
        try {
            return joinPoint.proceed()
        } finally {
            if (changeDatasource) {
                DynamicDataSourceContextHolder.poll()
                log.debug("回退数据源,{0}", joinPoint.target.javaClass.packageName)
            }
        }
    }

    /**
     * 决定本次调用是否切换数据源，并把决定写入 baomidou 的 ThreadLocal 栈。
     * 返回值：true 表示已 push，本切面 finally 需要 pop；false 表示没动栈。
     */
    private fun changeDatasource(serviceClazz: Class<*>): Boolean {
        if (forceChangeMain()) {
            return true
        }
        //找不到包所对应的数据源配置，则适用默认
        val dsKeyConfig: String = dataSourceProperties.lookDataSourceKey(serviceClazz)
        if (dsKeyConfig.isBlank()) {
            return false
        }
        //约定 指定_context则为数据源表，_context无需配置数据源，自动会从datasource里获取
        if (dsKeyConfig.startsWith(CONTEXT_DATASOURCE)) {
            val datasourcePair = convertDatasourceConfig(dsKeyConfig)
            //兼容多租户，不同上下文的数据源不同
            val mapKey: String = DatasourceKeyTool.convertCacheMapKey(
                checkNotNull(datasourcePair.first) { "datasource config first must not be null" },
                KudosContextHolder.get().dataSourceId, datasourcePair.second
            )
            //如果已经切换过了数据源，则缓存起来
            var dsKey: String? = dsCacheMap[mapKey]
            if (dsKey.isNullOrBlank()) {
                READ_WRITE_LOCK.readLock().lock()
                try {
                    dsKey = dsCacheMap.computeIfAbsent(mapKey) { k ->
                        requireNotNull(dsContextProcessor.doDetermineDatasource(k, dsKeyConfig)) {
                            "doDetermineDatasource returned null for $k"
                        }
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
     * 处理 `DbContext.forcedDs` 这一类"显式强制切换"的快速路径。返回 true 表示已切换
     * 并完成 push，调用方不需要再走包路径匹配；false 表示这条路径不适用，继续后续路由。
     *
     * 跳过条件：
     *  - forcedDs 为空 → 没有强制意图
     *  - 是 readonly 或 `_context` 前缀 → 这类语义不在本快速路径里处理，留给后续动态解析
     *  - forcedDs 指向的数据源在路由表里不存在 → 跳过，避免 push 不可达 key
     */
    private fun forceChangeMain(): Boolean {
        val forcedDs = DbContext.get().forcedDs
        if (forcedDs.isNullOrBlank()) {
            return false
        }
        if (DbContext.get().readonly || forcedDs.startsWith(CONTEXT_DATASOURCE)) {
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


    /**
     * 把 `_context` 前缀的数据源配置 + 当前 `DbParam` 拆成一个 (key, suffix) 二元组，
     * 供 [DatasourceKeyTool.convertCacheMapKey] 生成最终的缓存 key。
     * - readonly 强制时：suffix = MODE_READONLY，key 仍是原配置
     * - 非 readonly 但有 forcedDs：把 forcedDs 当 key 用（适配 TenantDsChange）
     * - 都没有：默认走 master
     */
    private fun convertDatasourceConfig(dsKeyConfig: String?): Pair<String?, String?> {
        val mapKeySuffix = DatasourceConst.MODE_MASTER
        val forceDs = DbContext.get().forcedDs
        //动态数据源才需要去判断强制切换
        if (!DbContext.get().forcedDs.isNullOrBlank()) {
            return if (DbContext.get().readonly) {
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
        private val log = LogFactory.getLog(this::class)
        private val dsCacheMap = ConcurrentHashMap<String, String>()

        private val READ_WRITE_LOCK: ReadWriteLock = ReentrantReadWriteLock()

        /**
         * 清空"包路径 → 真实数据源 key"的解析缓存。在租户数据源动态变更（增 / 改）后调用，
         * 让下次调用重新跑 [DsContextProcessor.doDetermineDatasource]。
         * `@Synchronized` + writeLock 双重保护是历史遗留，writeLock 已经够；
         * synchronized 是冗余的，留着不删避免对外行为变。
         */
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
