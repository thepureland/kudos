package io.kudos.ability.data.rdb.flyway.multidatasource

/**
 * Multi-data-source Flyway configuration properties, mapped to `kudos.ability.flyway.*` in yml.
 *
 * Direction: **data source key → list of module names**. A single data source may host multiple
 * modules, declared as either a CSV string or a YAML list. Declaration order is preserved
 * ([LinkedHashMap]); within a single data source, modules migrate in that order. Across data
 * sources, migration order follows the declaration order of [datasourceConfig], unless
 * [executionOrder] explicitly overrides it.
 *
 * Example yml:
 * ```yaml
 * kudos:
 *   ability:
 *     flyway:
 *       datasource-config:
 *         master: sys,tenant     # CSV form
 *         audit:                 # list form
 *           - audit_log
 *       execution-order:         # optional explicit ordering
 *         - master
 *         - audit
 *       auto-config:
 *         enabled: false         # if true, all sql/<x>/ on classpath must be declared above
 * ```
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class FlywayMultiDataSourceProperties {

    /**
     * Data source → module list. Value accepts a CSV string (`"sys,tenant"`) or a YAML list
     * (`- sys`, `- tenant`); both are normalized by [getDatasourceModules].
     */
    var datasourceConfig: LinkedHashMap<String, Any?> = linkedMapOf()

    /**
     * Optional override of data-source execution order. When non-empty, listed keys run first in
     * the given order; data sources not listed keep their declaration order and follow at the end.
     * Entries that don't match any key in [datasourceConfig] are silently ignored.
     */
    var executionOrder: List<String> = emptyList()

    /** Auto-discovery toggle (see [AutoConfig]). */
    var autoConfig: AutoConfig = AutoConfig()

    /**
     * Normalized view: data source → list of modules (declaration order). Values are parsed from
     * CSV or List into [List]<[String]>. Reserved keys (see [isReservedDatasourceConfigKey]) are
     * filtered out to defend against YAML mis-indentation (e.g. `execution-order` accidentally
     * nested under `datasource-config`).
     */
    fun getDatasourceModules(): LinkedHashMap<String, List<String>> {
        val result = LinkedHashMap<String, List<String>>()
        datasourceConfig.forEach { (ds, modules) ->
            if (isReservedDatasourceConfigKey(ds)) return@forEach
            result[ds] = parseModules(modules)
        }
        return result
    }

    /** Returns the data source key hosting [moduleName], or `null` if no entry contains it. */
    fun getDataSourceKey(moduleName: String): String? {
        getDatasourceModules().forEach { (ds, modules) ->
            if (moduleName in modules) return ds
        }
        return null
    }

    /**
     * Accepts CSV string, YAML list, or null. When a YAML list sits under a `Map<String, Any?>`
     * value, Spring Boot's relaxed binder flattens it to an indexed `LinkedHashMap` (e.g.
     * `{0=moduleX, 1=other}`); the [Map] branch handles that case by taking `values()` in
     * insertion order. Blanks are trimmed and dropped.
     */
    private fun parseModules(value: Any?): List<String> = when (value) {
        null -> emptyList()
        is Iterable<*> -> value.mapNotNull { it?.toString()?.trim() }.filter { it.isNotEmpty() }
        is Map<*, *> -> value.values.mapNotNull { it?.toString()?.trim() }.filter { it.isNotEmpty() }
        else -> value.toString()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /** Auto-config toggle. When `enabled=true` every module discovered on classpath must appear in [datasourceConfig]. */
    open class AutoConfig {
        var enabled: Boolean = false
    }

    companion object {
        /**
         * Same name as [executionOrder]; protects against YAML mis-indentation where the list ends
         * up flattened under `datasource-config` (e.g. `datasource-config.execution-order[0]`).
         */
        const val EXECUTION_ORDER_RESERVED_KEY = "execution-order"

        /** Whether [key] is a reserved property name accidentally captured under `datasource-config`. */
        fun isReservedDatasourceConfigKey(key: String?): Boolean {
            if (key.isNullOrBlank()) return false
            return key == EXECUTION_ORDER_RESERVED_KEY
                || key.startsWith("$EXECUTION_ORDER_RESERVED_KEY[")
        }
    }
}
