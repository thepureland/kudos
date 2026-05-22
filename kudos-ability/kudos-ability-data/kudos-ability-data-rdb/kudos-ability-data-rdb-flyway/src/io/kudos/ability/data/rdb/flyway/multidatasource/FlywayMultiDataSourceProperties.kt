package io.kudos.ability.data.rdb.flyway.multidatasource

/**
 * 多数据源 Flyway 配置属性，对应 yml 里 `kudos.ability.flyway.*`。
 *
 * 主要承载"模块名 → 数据源 key"的映射 [datasourceConfig]，启动时
 * [FlywayMultiDataSourceMigrator] 据此决定哪些模块需要迁移、用哪个数据源。
 *
 * 用 [LinkedHashMap] 保留声明顺序 —— 同一启动周期内模块迁移按 yml 声明顺序进行，
 * 便于处理"模块 A 的 schema 必须先于模块 B 落地"这类显式依赖。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class FlywayMultiDataSourceProperties {

    /** 数据源配置信息：key = 模块名（对应 `sql/<moduleName>/` 目录），value = 动态数据源 key。 */
    var datasourceConfig: LinkedHashMap<String, String> = linkedMapOf()

    /** 取某模块对应的数据源 key；模块未配置时返回 `null`。 */
    fun getDataSourceKey(moduleName: String): String? = datasourceConfig[moduleName]
}
