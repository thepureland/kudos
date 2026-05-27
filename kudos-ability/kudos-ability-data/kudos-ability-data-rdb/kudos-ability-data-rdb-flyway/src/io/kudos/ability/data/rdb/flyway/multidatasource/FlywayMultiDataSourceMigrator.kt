package io.kudos.ability.data.rdb.flyway.multidatasource

import io.kudos.ability.data.rdb.flyway.kit.FlywayKit
import io.kudos.ability.data.rdb.jdbc.datasource.DsContextProcessor
import io.kudos.base.io.FileKit
import io.kudos.base.io.scanner.classpath.ClassPathScanner
import io.kudos.base.logger.LogFactory
import io.kudos.context.config.YamlPropertySourceFactory
import jakarta.annotation.Resource
import org.springframework.boot.flyway.autoconfigure.FlywayProperties
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.EnumerablePropertySource
import java.io.File


/**
 * Multi-data-source Flyway script upgrader.
 *
 * Conventions / behavior:
 * 1. SQL scripts are organized on the classpath by "module name + database type", e.g.
 *    `sql/<moduleName>/<dbType>/V1.0.1__xxx.sql`, where `<dbType>` is the lowercase form of
 *    `io.kudos.ability.data.rdb.jdbc.consts.RdbTypeEnum#name`.
 * 2. SQL script filenames follow the Flyway convention (V_x__xxx.sql / R__xxx.sql ...).
 * 3. The migration plan is `dataSource → [modules]`; within a data source modules run in
 *    declaration order; across data sources, the order follows [FlywayMultiDataSourceProperties.executionOrder]
 *    when set, otherwise the declaration order of `datasource-config`.
 * 4. In **manual mode** (`auto-config.enabled=false`, default) only modules listed in
 *    `datasource-config` are migrated; missing-on-disk modules are warned and skipped.
 * 5. In **auto mode** (`auto-config.enabled=true`) every `sql/<x>/` directory on the classpath
 *    must appear under some data source in `datasource-config`; otherwise startup aborts
 *    (the assumption being that auto only relaxes scanning, not the ds mapping decision).
 * 6. If a module name appears under multiple classpath URLs (e.g. spread across jars), startup
 *    aborts — Flyway can't reconcile a single history table sourced from two origins.
 * 7. Any module's migration failure interrupts the rest (exception from [FlywayKit] propagates).
 * 8. Supports different RDB types in one pass (`dbType` is detected from each module's data source).
 *
 * Coupling: data source instances are resolved via [DsContextProcessor], so this currently
 * **depends on the dynamic-routing data source provided by the baomidou dynamic-datasource starter**.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class FlywayMultiDataSourceMigrator {

    @Resource
    private lateinit var flywayMultiDatasourceProperties: FlywayMultiDataSourceProperties

    @Resource
    private lateinit var flywayProperties: FlywayProperties

    @Resource
    private lateinit var dsContextProcessor: DsContextProcessor

    @Resource
    private lateinit var environment: ConfigurableEnvironment

    private val log = LogFactory.getLog(this::class)

    /**
     * Main entry point: scan the classpath, reconcile with declared `datasource-config`, build
     * an ordered `ds → [modules]` plan, then migrate. Any per-module failure propagates so Spring
     * startup is interrupted instead of running against a half-migrated schema.
     */
    fun migrate() {
        val plan = buildExecutionPlan()
        if (plan.isEmpty()) {
            log.info("kudos-ability-data-rdb-flyway: no Flyway modules to execute (no sql/<module>/ on classpath, or datasource-config is empty)")
            return
        }
        val totalModules = plan.values.sumOf { it.size }
        log.info("kudos-ability-data-rdb-flyway: discovered $totalModules Flyway module(s); execution plan by data source: $plan")
        plan.forEach { (ds, modules) ->
            modules.forEach { module -> migrateByModule(module, ds) }
        }
    }

    /**
     * Single-module entry. Looks up the configured data source for [moduleName] from
     * `datasource-config` and migrates that one module. If the module has no configured data
     * source, throws with a hint pointing at the originating yml file(s) — see
     * [resolveDatasourceConfigSources].
     */
    fun migrateByModule(moduleName: String) {
        val datasourceKey = flywayMultiDatasourceProperties.getDataSourceKey(moduleName)
            ?: run {
                val sources = resolveDatasourceConfigSources().ifEmpty { listOf("unknown") }
                error("Module [$moduleName] has no data source configured under kudos.ability.flyway.datasource-config (configuration sources: $sources)")
            }
        migrateByModule(moduleName, datasourceKey)
    }

    /**
     * 2-arg variant: validates that [datasourceKey] actually exists in the dynamic routing table
     * before handing off to [FlywayKit]. Exposed `internal` so tests and adjacent callers (e.g.
     * code generators inside the same module) can drive a specific (module, ds) pair.
     */
    internal fun migrateByModule(moduleName: String, datasourceKey: String) {
        if (!dsContextProcessor.haveDataSource(datasourceKey)) {
            val sources = resolveDatasourceConfigSources().ifEmpty { listOf("unknown") }
            val errMsg = "The data source [$datasourceKey] configured for module [$moduleName] does not exist! All subsequent database updates are aborted! (configuration sources: $sources)"
            log.error(errMsg)
            error(errMsg)
        }

        val dataSource = checkNotNull(dsContextProcessor.getDataSource(datasourceKey)) {
            "Data source [$datasourceKey] resolved to null"
        }
        FlywayKit.migrate(moduleName, dataSource, flywayProperties)
    }

    /**
     * Build the ordered `ds → [modules]` plan.
     *
     * - Manual mode: keep only the intersection of (configured ∩ on-disk); orphans declared but
     *   missing on disk are warned and skipped; modules on disk but absent from config are
     *   silently ignored.
     * - Auto mode: every on-disk module must appear in some ds entry; otherwise abort (treat
     *   missing mapping as a configuration bug rather than guessing a primary data source).
     *
     * Final ordering applies [FlywayMultiDataSourceProperties.executionOrder] when present.
     */
    private fun buildExecutionPlan(): LinkedHashMap<String, List<String>> {
        val configured = flywayMultiDatasourceProperties.getDatasourceModules()
        val auto = flywayMultiDatasourceProperties.autoConfig.enabled
        val moduleNamesOnDisk = scanModuleNamesFromClasspath()

        if (auto) {
            val configuredModules = configured.values.flatten().toSet()
            val unmapped = moduleNamesOnDisk - configuredModules
            if (unmapped.isNotEmpty()) {
                val sources = resolveDatasourceConfigSources().ifEmpty { listOf("unknown") }
                error(
                    "kudos.ability.flyway.auto-config.enabled=true requires every classpath sql/<module>/ to be declared under datasource-config." +
                        " Missing mapping for module(s): $unmapped (configuration sources: $sources)"
                )
            }
        }

        val plan = linkedMapOf<String, List<String>>()
        configured.forEach { (ds, modules) ->
            val (onDisk, orphan) = modules.partition { it in moduleNamesOnDisk }
            if (orphan.isNotEmpty()) {
                log.warn("Modules configured for data source [$ds] not found under classpath sql/: $orphan")
            }
            if (onDisk.isNotEmpty()) plan[ds] = onDisk
        }
        return applyExecutionOrder(plan)
    }

    /**
     * Reorder data sources according to [FlywayMultiDataSourceProperties.executionOrder]. Listed
     * keys come first in the given order; unlisted keys keep their original relative order and
     * follow at the end; entries in the order list that don't exist in [plan] are dropped.
     */
    private fun applyExecutionOrder(plan: LinkedHashMap<String, List<String>>): LinkedHashMap<String, List<String>> {
        val order = flywayMultiDatasourceProperties.executionOrder
        if (order.isEmpty()) return plan
        val remaining = LinkedHashSet(plan.keys)
        val ordered = LinkedHashMap<String, List<String>>()
        order.forEach { ds ->
            val key = ds.trim()
            if (key.isBlank()) return@forEach
            if (remaining.remove(key)) {
                ordered[key] = plan.getValue(key)
            }
        }
        remaining.forEach { ordered[it] = plan.getValue(it) }
        return ordered
    }

    /**
     * Scan direct subdirectories of `sql/` on the classpath. Duplicate-detection semantics:
     * if a module name appears under multiple classpath URLs (different jars / source sets),
     * an [IllegalStateException] is thrown — Flyway can't merge two origins into a single
     * schema history table.
     */
    private fun scanModuleNamesFromClasspath(): Set<String> {
        val sqlRootPath = FlywayKit.SQL_ROOT_PATH
        val locationUrls = ClassPathScanner.getLocationUrlsForPath(sqlRootPath)
        val moduleNames = linkedSetOf<String>()
        locationUrls.forEach { url ->
            val childFolders = listChildFolders(url, sqlRootPath)
            childFolders.forEach { moduleName ->
                if (moduleNames.contains(moduleName)) {
                    error("Multiple modules named [$moduleName] exist! Please check whether the classpath contains duplicate sql/$moduleName directories.")
                }
                moduleNames.add(moduleName)
            }
        }
        return moduleNames
    }

    /**
     * Return the names of the direct subdirectories of [sqlRootPath] under a given classpath URL;
     * automatically handles both jar and filesystem protocols.
     */
    private fun listChildFolders(url: java.net.URL, sqlRootPath: String): List<String> {
        return if (url.protocol == "jar") {
            val paths = FileKit.listFilesOrDirsInJar(url.toString().removeSuffix(sqlRootPath), sqlRootPath)
            paths.map { it.removePrefix("$sqlRootPath/").removeSuffix("/") }
        } else {
            File(url.path).listFiles()?.map { it.name }.orEmpty()
        }
    }

    /**
     * Locate which yml file(s) contributed any `kudos.ability.flyway.datasource-config.*` entry.
     * Used in error messages to help debug multi-jar deployments where the offending value may
     * have come from a dependency's yml rather than the application's. Relies on
     * [YamlPropertySourceFactory.getSourceMap] populated as yml files are loaded.
     */
    private fun resolveDatasourceConfigSources(): List<String> {
        val prefix = "kudos.ability.flyway.datasource-config."
        val sourceMap = YamlPropertySourceFactory.getSourceMap()
        val locations = linkedSetOf<String>()
        for (propertySource in environment.propertySources) {
            if (propertySource !is EnumerablePropertySource<*>) continue
            val matched = propertySource.propertyNames.any { it.startsWith(prefix) }
            if (!matched) continue
            locations.add(sourceMap[propertySource.name] ?: propertySource.name)
        }
        return locations.toList()
    }
}
