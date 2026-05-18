package io.kudos.ability.data.rdb.jdbc.datasource

import com.baomidou.dynamic.datasource.creator.DataSourceCreator
import com.baomidou.dynamic.datasource.creator.DataSourceProperty
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator
import com.baomidou.dynamic.datasource.ds.ItemDataSource
import com.baomidou.dynamic.datasource.enums.SeataMode
import com.baomidou.dynamic.datasource.event.DataSourceInitEvent
import com.baomidou.dynamic.datasource.support.ScriptRunner
import com.baomidou.dynamic.datasource.toolkit.CryptoUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import javax.sql.DataSource

/**
 * 自定义的 baomidou [DefaultDataSourceCreator]，在 baomidou 的基础上加入：
 *  - 启动时根据 [DataSourceProperty.init] 执行 schema/data 脚本（[runScrip]）
 *  - 通过 [IDataSourceProxy] 把创建出来的 DataSource 包一层（[wrapDataSource]）
 *  - 当代理是 Seata 模式时**强制把 autoCommit 全设为 true**（见 [createDataSource] 内注释）
 *
 * 接 Spring 容器单例（`@Component`），由 baomidou 自动通过 `setCreators` 注入下游
 * 各 DataSource 类型（Hikari / Druid / DBCP2 / BeeCP）的具体 creator。
 *
 * @author K
 * @since 1.0.0
 */
@Component
class DsDataSourceCreator : DefaultDataSourceCreator() {

    private var creators: List<DataSourceCreator>? = null

    @Autowired(required = false)
    private val dataSourceProxy: IDataSourceProxy? = null

    /** 是否懒加载数据源（baomidou 全局 lazy 配置）。 */
    private var lazy: Boolean? = false

    /** 是否启用 p6spy SQL 日志输出（baomidou 全局 p6spy 配置）。 */
    private var p6spy = false

    /** 全局默认 publicKey，用于 baomidou 加密 / 解密敏感配置项（如 password）。 */
    private var publicKey: String? = CryptoUtils.DEFAULT_PUBLIC_KEY_STRING

    /** baomidou 提供的 DataSource 初始化事件钩子；可空。 */
    private var dataSourceInitEvent: DataSourceInitEvent? = null

    /**
     * 创建数据源。流程：
     *  1. 从注册的多种 creator 里挑出 support 当前 [DataSourceProperty] 的那个（Hikari/Druid/...）
     *  2. **Seata 兼容**：如果代理是 Seata 模式，强制把 autoCommit 设 true 覆盖任何 yml 配置。
     *     Seata AT 模式靠每条 SQL 自动 commit 时 ConnectionProxy 的拦截链来写 undo log /
     *     register branch；autoCommit=false 时拦截链根本不触发，写最后会被还池时回滚。详见
     *     `kudos-ability-distributed-tx-seata` 模块的故障调查记录。
     *  3. 解密 publicKey / 设置 lazy 默认值
     *  4. 走 [DataSourceInitEvent] 的 beforeCreate → creator.createDataSource → afterCreate
     *  5. 执行 schema / data 初始化脚本
     *  6. 包装代理 + 打 ItemDataSource 元信息
     */
    override fun createDataSource(dataSourceProperty: DataSourceProperty): DataSource {
        val dataSourceCreator = checkNotNull(
            checkNotNull(creators) { "creators must be set" }
                .firstOrNull { it.support(dataSourceProperty) }
        ) { "creator must not be null,please check the DataSourceCreator" }
        if (dataSourceProxy != null && dataSourceProxy.isSeata()) {
            // !! Seata AT 模式的硬约束 —— 不要随意删 !!
            // Seata 的 ConnectionProxy 只有在每条 SQL 自动 commit 时才会触发 undo-log 写入 +
            // BranchRegister。autoCommit=false 会让所有 SQL 卡在隐式开放事务里，最终被
            // Hikari 还池时回滚，数据全丢。
            dataSourceProperty.druid?.defaultAutoCommit = true
            dataSourceProperty.hikari?.isAutoCommit = true
            dataSourceProperty.beecp?.defaultAutoCommit = true
            dataSourceProperty.dbcp2?.defaultAutoCommit = true
        }
        val propertyPublicKey = dataSourceProperty.publicKey
        if (propertyPublicKey.isEmpty()) {
            dataSourceProperty.publicKey = publicKey
        }
        val propertyLazy = dataSourceProperty.lazy
        if (propertyLazy == null) {
            dataSourceProperty.lazy = lazy
        }
        dataSourceInitEvent?.beforeCreate(dataSourceProperty)
        val dataSource = dataSourceCreator.createDataSource(dataSourceProperty)
        dataSourceInitEvent?.afterCreate(dataSource)
        this.runScrip(dataSource, dataSourceProperty)
        return wrapDataSource(dataSource, dataSourceProperty)
    }

    /**
     * 启动期为新建数据源执行 schema / data 脚本（如果 [DataSourceProperty.init] 里有声明）。
     * baomidou 的 [ScriptRunner] 一条条执行 SQL；schema 先于 data。
     */
    private fun runScrip(dataSource: DataSource?, dataSourceProperty: DataSourceProperty) {
        val initProperty = dataSourceProperty.init
        val schema = initProperty.schema
        val data = initProperty.data
        if (StringUtils.hasText(schema) || StringUtils.hasText(data)) {
            val scriptRunner = ScriptRunner(initProperty.isContinueOnError, initProperty.separator)
            if (StringUtils.hasText(schema)) {
                scriptRunner.runScript(dataSource, schema)
            }
            if (StringUtils.hasText(data)) {
                scriptRunner.runScript(dataSource, data)
            }
        }
    }

    /**
     * 把原始 DataSource 通过 [IDataSourceProxy] 包一层（典型是 Seata 代理），并构造
     * baomidou 的 [ItemDataSource] —— 保留原 / 代理两个引用 + Seata 模式信号位 + p6spy 标记。
     */
    private fun wrapDataSource(dataSource: DataSource, dataSourceProperty: DataSourceProperty): DataSource {
        val name = dataSourceProperty.poolName
        var targetDataSource = dataSource
        var isSeata = false
        var seataMode: SeataMode? = null
        if (dataSourceProxy != null) {
            targetDataSource = dataSourceProxy.proxyDatasource(dataSource)
            isSeata = dataSourceProxy.isSeata()
            seataMode = dataSourceProxy.seataMode()
        }
        val enabledP6spy = p6spy && dataSourceProperty.p6spy
        return ItemDataSource(name, dataSource, targetDataSource, enabledP6spy, isSeata, seataMode)
    }

    /** baomidou 注入下游各类型 DataSourceCreator 列表的入口。 */
    override fun setCreators(creators: List<DataSourceCreator>) {
        this.creators = creators
    }

    /** 设置全局 lazy 标记。 */
    override fun setLazy(lazy: Boolean?) {
        this.lazy = lazy
    }

    /** 设置全局 p6spy 标记。 */
    override fun setP6spy(p6spy: Boolean) {
        this.p6spy = p6spy
    }

    /** 设置全局 publicKey（用于 baomidou 的敏感配置解密）。 */
    override fun setPublicKey(publicKey: String?) {
        this.publicKey = publicKey
    }

    /** 设置 baomidou DataSource 初始化事件钩子。 */
    override fun setDataSourceInitEvent(dataSourceInitEvent: DataSourceInitEvent?) {
        this.dataSourceInitEvent = dataSourceInitEvent
    }
}
