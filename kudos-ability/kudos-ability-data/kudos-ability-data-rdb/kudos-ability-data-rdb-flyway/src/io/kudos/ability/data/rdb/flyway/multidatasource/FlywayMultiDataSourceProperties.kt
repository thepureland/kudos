package io.kudos.ability.data.rdb.flyway.multidatasource

/**
 * Multi-data-source Flyway configuration properties, mapped to `kudos.ability.flyway.*` in yml.
 *
 * Primarily carries the "module name → data source key" mapping [datasourceConfig]; at startup
 * [FlywayMultiDataSourceMigrator] uses it to decide which modules need migration and which data
 * source to use.
 *
 * Uses [LinkedHashMap] to preserve declaration order — within a single startup cycle modules are
 * migrated in the order declared in yml, which makes it easy to express explicit dependencies like
 * "module A's schema must land before module B's".
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class FlywayMultiDataSourceProperties {

    /** Data source configuration: key = module name (matches the `sql/<moduleName>/` directory), value = dynamic data source key. */
    var datasourceConfig: LinkedHashMap<String, String> = linkedMapOf()

    /** Returns the data source key for the given module; returns `null` if the module is not configured. */
    fun getDataSourceKey(moduleName: String): String? = datasourceConfig[moduleName]
}
