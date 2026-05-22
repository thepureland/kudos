package io.kudos.ability.data.rdb.jdbc.datasource

import com.baomidou.dynamic.datasource.enums.SeataMode
import javax.sql.DataSource

/**
 * 数据源代理扩展点。
 *
 * 在 [DsDataSourceCreator] 创建每个动态数据源后调用 [proxyDatasource] 让外部"包"一层
 * 自己的代理（典型应用：Seata 的 `DataSourceProxy` 拦截 commit/rollback 实现 AT 模式）。
 *
 * - 不实现时（容器里没有 bean）：返回原 datasource，相当于无代理
 * - [isSeata] / [seataMode] 信号给 baomidou 内部用来决定 ItemDataSource 的标记位
 *
 * 当前只有 Seata 模块（kudos-ability-distributed-tx-seata）会注册 `SeataDataSourceProxy`
 * 实例进容器；其它项目可以注册自己的代理（监控 / 加密 / 审计等）。
 *
 * @author damon
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IDataSourceProxy {
    /**
     * 代理 / 包装一个数据源。默认实现是直接返回原数据源（"无代理"）。
     */
    fun proxyDatasource(dataSource: DataSource): DataSource {
        return dataSource
    }

    /** 是否启用 Seata 集成；baomidou 的 `ItemDataSource` 据此设置标记位。 */
    fun isSeata(): Boolean = false

    /** Seata 模式（AT / XA / TCC / SAGA），仅 [isSeata] 为 true 时有意义。 */
    fun seataMode(): SeataMode? {
        return null
    }
}
