package io.kudos.ability.data.rdb.flyway.multidatasource

import io.kudos.ability.data.rdb.flyway.kit.FlywayConfig

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
     * Execution mode: [MODE_MIGRATE] (default) applies migrations at startup; [MODE_DRY_RUN] only
     * logs each module's pending migrations without touching any schema (preview for ops).
     */
    var mode: String = MODE_MIGRATE

    /** Global Flyway parameters (the supported contract — see [FlywayConfig]). */
    var flywayConfig: FlywayConfig = FlywayConfig()

    /**
     * Optional per-data-source overrides of [flywayConfig], keyed by the same data source keys as
     * [datasourceConfig]. Unset fields inherit the global value; [FlywayOverrides.placeholders]
     * is merged over the global placeholders per key.
     */
    var datasourceFlywayConfig: LinkedHashMap<String, FlywayOverrides> = linkedMapOf()

    /** Whether [mode] selects the dry-run behavior; rejects unknown values loudly. */
    fun isDryRun(): Boolean = when (mode.trim().lowercase().replace('_', '-')) {
        MODE_MIGRATE -> false
        MODE_DRY_RUN -> true
        else -> error("Unknown kudos.ability.flyway.mode [$mode]; supported values: $MODE_MIGRATE, $MODE_DRY_RUN")
    }

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

    /**
     * Per-data-source overrides of [FlywayConfig]. Every field is nullable — `null` means
     * "inherit the global value"; [placeholders] is the exception: it is merged over the global
     * map per key instead of replacing it.
     */
    open class FlywayOverrides {
        var baselineOnMigrate: Boolean? = null
        var baselineVersion: String? = null

        /** Charset name (e.g. `UTF-8`). */
        var encoding: String? = null
        var outOfOrder: Boolean? = null
        var validateOnMigrate: Boolean? = null
        var failOnMissingLocations: Boolean? = null
        var cleanDisabled: Boolean? = null
        var placeholderReplacement: Boolean? = null
        var placeholders: LinkedHashMap<String, String> = linkedMapOf()
        var placeholderPrefix: String? = null
        var placeholderSuffix: String? = null
        var placeholderSeparator: String? = null

        /**
         * Produce a new [FlywayConfig] combining [base] with this override; [base] itself is
         * never mutated.
         */
        fun mergedInto(base: FlywayConfig): FlywayConfig {
            val merged = base.copy()
            baselineOnMigrate?.let { merged.baselineOnMigrate = it }
            baselineVersion?.let { merged.baselineVersion = it }
            encoding?.let { merged.encoding = it }
            outOfOrder?.let { merged.outOfOrder = it }
            validateOnMigrate?.let { merged.validateOnMigrate = it }
            failOnMissingLocations?.let { merged.failOnMissingLocations = it }
            cleanDisabled?.let { merged.cleanDisabled = it }
            placeholderReplacement?.let { merged.placeholderReplacement = it }
            merged.placeholders.putAll(placeholders)
            placeholderPrefix?.let { merged.placeholderPrefix = it }
            placeholderSuffix?.let { merged.placeholderSuffix = it }
            placeholderSeparator?.let { merged.placeholderSeparator = it }
            return merged
        }
    }

    companion object {
        /** [mode] value: apply migrations at startup (default). */
        const val MODE_MIGRATE = "migrate"

        /** [mode] value: only log pending migrations, never touch any schema. */
        const val MODE_DRY_RUN = "dry-run"

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
