package io.kudos.ability.data.rdb.flyway.multidatasource

/**
 * 多数据源Flyway属性
 *
 * @author K
 * @since 1.0.0
 */
open class FlywayMultiDataSourceProperties {

    /**
     * 数据源配置信息：key=模块名，value=数据源名
     */
    var datasourceConfig = linkedMapOf<String, String>()

    fun getDataSourceKey(moduleName: String): String? = datasourceConfig[moduleName]

}