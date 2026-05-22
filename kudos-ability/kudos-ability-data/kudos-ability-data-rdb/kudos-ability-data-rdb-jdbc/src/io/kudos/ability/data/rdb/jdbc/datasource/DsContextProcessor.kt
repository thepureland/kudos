package io.kudos.ability.data.rdb.jdbc.datasource

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource
import com.baomidou.dynamic.datasource.creator.DataSourceProperty
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator
import io.kudos.ability.data.rdb.jdbc.aop.DynamicDataSourceAspect
import io.kudos.ability.data.rdb.jdbc.consts.DatasourceConst
import io.kudos.ability.data.rdb.jdbc.kit.DatasourceKeyTool
import io.kudos.base.logger.LogFactory
import io.kudos.base.support.KeyLockRegistry
import io.kudos.context.core.KudosContextHolder
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.sql.DataSource

/**
 * 把"上下文 dataSourceId"翻译成 baomidou 动态路由表里真正可用的数据源 key 的处理器。
 *
 * 角色：[DynamicDataSourceAspect] 命中 `_context::*` 类型的路由意图后，调用本处理器把
 * 当前上下文（租户 id、服务编码、master/readonly 模式）解析成一个具体 dsKey，然后查路由
 * 表 —— 如果该 dsKey 对应的 DataSource 还没被加载到路由表，就走 [IDynamicDataSourceLoad]
 * 拿配置 + [DsDataSourceCreator] 现场创建 + [IDataSourceProxy] 包代理 + 注册回路由表。
 *
 * 单数据源场景（注入的 `dataSource` 不是 [DynamicRoutingDataSource]）下，[getDataSource]
 * / [haveDataSource] / [currentDataSource] 都退化成"返回唯一数据源 / 始终为 true"。
 *
 * @author damon
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class DsContextProcessor {

    @Resource
    private lateinit var dataSource: DataSource

    @Resource
    private lateinit var dataSourceCreator: DefaultDataSourceCreator

    @Resource
    private lateinit var dynamicDataSourceLoad: IDynamicDataSourceLoad

    @Autowired(required = false)
    private val dataSourceProxy: IDataSourceProxy? = null

    @Autowired(required = false)
    private val dataSourceFinder: IDataSourceFinder? = null

    @Value($$"${spring.datasource.dynamic.primary:master}")
    private val primary: String? = null

    private val keyLockRegistry = KeyLockRegistry<String>()

    /**
     * 主入口：把 [DynamicDataSourceAspect] 计算出的 cache map key 翻译成路由表里的真实
     * dsKey。返回 `null` 表示"当前没有上下文（如未登录请求）"或"控制台租户，跳过路由"。
     *
     * 流程：
     *  1. 取 [KudosContextHolder] 的快照（`getOrNull` 不会污染 ThreadLocal —— 关键，详见
     *     上次修过的反模式）；快照为 null 直接返回 null
     *  2. 控制台租户（[DatasourceConst.CONSOLE_TENANT_ID]）跳过路由
     *  3. 默认 dsId = context.dataSourceId / mode = master；如果 dsKey 是 readOnly 后缀，
     *     则切到 context.readOnlyDataSourceId / mode = readonly
     *  4. 用 [keyLockRegistry] 按 dsKey 加锁（避免同 key 并发创建 DataSource 时重复加载）
     *  5. 如果有 [dataSourceFinder]，让业务方按"租户 + 服务 + 模式"覆盖出真实 dsId
     *  6. 最终走 [getDatasourceKey] 确保该 dsId 对应的 DataSource 已注册到路由表
     */
    fun doDetermineDatasource(dsKey: String, dsKeyConfig: String?): String? {
        val context = KudosContextHolder.getOrNull() ?: return null
        if (context._datasourceTenantId == DatasourceConst.CONSOLE_TENANT_ID) return null
        // 备库 dsKey 后缀触发切到 readOnlyDataSourceId / readonly 模式；否则用上下文的主库 id + master
        val (defaultDsId, mode) = if (DatasourceKeyTool.isReadOnly(dsKey) && context.readOnlyDataSourceId != null) {
            context.readOnlyDataSourceId to DatasourceConst.MODE_READONLY
        } else {
            context.dataSourceId to DatasourceConst.MODE_MASTER
        }
        keyLockRegistry.tryLock(dsKey)
        try {
            val realDsId = dataSourceFinder
                ?.findDataSourceId(context._datasourceTenantId, DatasourceKeyTool.getServerCode(dsKey), mode)
                ?: defaultDsId
            return getDatasourceKey(realDsId)
        } finally {
            keyLockRegistry.unlock(dsKey)
        }
    }

    /**
     * 确保某 dsId 对应的 DataSource 已经存在于 baomidou 路由表里，没有的话现场创建并注册，
     * 然后把 dsId 当 key 原样返回。失败（[IDynamicDataSourceLoad] 找不到配置）会抛
     * [RuntimeException]。
     *
     * `protected` —— 留给子类覆盖（例如想用别的方式从路由表里获取/注册）；外部不直接调用。
     */
    protected fun getDatasourceKey(dsId: String?): String? { //TODO
        val ds = dataSource as DynamicRoutingDataSource
        if (!ds.dataSources.containsKey(dsId)) {
            // 该dsKey数据源未初始化，加载配置并初始化
            val dsProperty = dynamicDataSourceLoad.getPropertyById(dsId)
                ?: run {
                    log.warn("动态数据源id未配置:{0}", dsId)
                    throw RuntimeException("动态数据源id未配置!dsId=$dsId")
                }
            log.warn("開始創建並載數據源id={0}...", dsId)
            val created = dataSourceCreator.createDataSource(dsProperty)
            val toRegister = dataSourceProxy?.proxyDatasource(created) ?: created
            ds.addDataSource(dsId, toRegister)
        }
        return dsId
    }

    /**
     * 按 dsKey 取真实数据源。路由表是 baomidou 的 [DynamicRoutingDataSource]，单数据源
     * 场景下 `dataSource` 不是这个类型，退化成"无视 key 直接返回唯一数据源"。
     */
    fun getDataSource(dsKey: String?): DataSource? =
        (dataSource as? DynamicRoutingDataSource)?.getDataSource(dsKey) ?: dataSource

    /**
     * 路由表里是否存在某 dsKey。单数据源场景始终返回 true（"只有这一个数据源"），多数据源
     * 场景查路由表内部 map。
     */
    fun haveDataSource(dsKey: String?): Boolean =
        (dataSource as? DynamicRoutingDataSource)?.dataSources?.containsKey(dsKey) ?: true

    /**
     * 刷新路由表里某 dsId 对应的数据源条目；`dsId == null` 表示"刷新所有除 primary 之外
     * 的数据源"。同时清空 [DynamicDataSourceAspect] 的解析缓存，让下一次路由解析重新走
     * [doDetermineDatasource]。
     *
     * 适用场景：租户数据源在元数据中心被修改后，对外通知重新加载。
     */
    fun refreshDatasource(dsId: Int?) {
        log.warn("收到刷新數據源id為：{0} 的請求", dsId)
        val ds = dataSource as DynamicRoutingDataSource
        if (dsId == null) {
            // 全量刷新：清掉 primary 以外的所有路由，再 afterPropertiesSet() 重建
            ds.dataSources.keys.filter { it != primary }.forEach(ds::removeDataSource)
            ds.afterPropertiesSet()
        } else {
            ds.removeDataSource(dsId.toString())
        }
        DynamicDataSourceAspect.cacheDsCache()
        log.warn("數據源刷新成功...")
    }

    /** 当前线程的"逻辑当前数据源"。多数据源场景委托给 baomidou 的 determineDataSource。 */
    fun currentDataSource(): DataSource? =
        (dataSource as? DynamicRoutingDataSource)?.determineDataSource() ?: dataSource

    private val log = LogFactory.getLog(this::class)

}
