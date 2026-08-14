package io.kudos.ability.data.rdb.flyway.multidatasource

import io.kudos.ability.data.rdb.flyway.kit.FlywayConfig
import io.kudos.ability.data.rdb.flyway.kit.FlywayKit
import io.kudos.ability.data.rdb.flyway.kit.IFlywayModuleConfigCustomizer
import io.kudos.base.io.FileKit
import io.kudos.base.io.scanner.classpath.ClassPathScanner
import io.kudos.base.logger.LogFactory
import io.kudos.context.config.YamlPropertySourceFactory
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.EnumerablePropertySource
import java.io.File
import java.util.concurrent.ConcurrentHashMap


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
 * 9. Flyway parameters come from `kudos.ability.flyway.flyway-config.*` ([FlywayConfig]), optionally
 *    overridden per data source; `spring.flyway.*` is NOT used and is warned about at startup.
 *
 * Data source instances are resolved via the [IFlywayDataSourceResolver] SPI; the default
 * implementation delegates to the baomidou dynamic-routing table, but any other multi-data-source
 * mechanism can plug in by registering its own resolver bean.
 *
 * Startup ordering: [io.kudos.ability.data.rdb.flyway.init.FlywayMigratorDatabaseInitializerDetector]
 * registers this class as a Spring Boot database initializer, so beans detected as depending on
 * database initialization are created only after migrations complete.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
open class FlywayMultiDataSourceMigrator {

    @Resource
    private lateinit var flywayMultiDatasourceProperties: FlywayMultiDataSourceProperties

    @Resource
    private lateinit var dataSourceResolver: IFlywayDataSourceResolver

    @Resource
    private lateinit var environment: ConfigurableEnvironment

    /** Per-module configuration hooks; empty when no such beans are registered (see [IFlywayModuleConfigCustomizer]). */
    @Autowired(required = false)
    private var moduleConfigCustomizers: List<IFlywayModuleConfigCustomizer> = emptyList()

    /** dbType per datasource key, so N modules on one data source detect it only once. */
    private val dbTypeCache = ConcurrentHashMap<String, String>()

    /** Effective (global + per-ds override) Flyway parameters per datasource key. */
    private val effectiveConfigCache = ConcurrentHashMap<String, FlywayConfig>()

    private val log = LogFactory.getLog(this::class)

    /**
     * Main entry point: scan the classpath, reconcile with declared `datasource-config`, build
     * an ordered `ds → [modules]` plan, then migrate. Any per-module failure propagates so Spring
     * startup is interrupted instead of running against a half-migrated schema.
     *
     * With `kudos.ability.flyway.mode=dry-run` the same plan is only previewed: each module's
     * pending migrations are logged and nothing is applied.
     */
    fun migrate() {
        warnOnIgnoredSpringFlywayKeys()
        val plan = buildExecutionPlan()
        if (plan.isEmpty()) {
            log.info("kudos-ability-data-rdb-flyway: no Flyway modules to execute (no sql/<module>/ on classpath, or datasource-config is empty)")
            return
        }
        val dryRun = flywayMultiDatasourceProperties.isDryRun()
        val totalModules = plan.values.sumOf { it.size }
        val dryRunHint = if (dryRun) " [dry-run: nothing will be applied]" else ""
        log.info("kudos-ability-data-rdb-flyway: discovered $totalModules Flyway module(s); execution plan by data source: $plan$dryRunHint")
        plan.forEach { (ds, modules) ->
            modules.forEach { module -> if (dryRun) previewByModule(module, ds) else migrateByModule(module, ds) }
        }
    }

    /**
     * Single-module entry. Looks up the configured data source for [moduleName] from
     * `datasource-config` and migrates that one module. If the module has no configured data
     * source, throws with a hint pointing at the originating yml file(s) — see
     * [resolveDatasourceConfigSources].
     */
    fun migrateByModule(moduleName: String) = migrateByModule(moduleName, requireDatasourceKey(moduleName))

    /**
     * 2-arg variant: validates that [datasourceKey] actually exists in the resolver's routing
     * table before handing off to [FlywayKit]. Public on purpose: besides tests and code
     * generators, it is the entry point for data sources registered at runtime (e.g. migrating a
     * newly onboarded tenant's data source without restarting).
     */
    fun migrateByModule(moduleName: String, datasourceKey: String) {
        val dataSource = resolveDataSource(moduleName, datasourceKey)
        val dbType = dbTypeCache.getOrPut(datasourceKey) { FlywayKit.detectDbType(dataSource) }
        FlywayKit.migrate(moduleName, dataSource, effectiveConfig(datasourceKey), dbType, moduleConfigCustomizers)
    }

    /**
     * Dry-run preview of a single (module, ds) pair: logs the pending migrations without applying
     * anything (see [FlywayKit.pendingMigrations] — `info()` never modifies the schema).
     */
    fun previewByModule(moduleName: String, datasourceKey: String) {
        val dataSource = resolveDataSource(moduleName, datasourceKey)
        val dbType = dbTypeCache.getOrPut(datasourceKey) { FlywayKit.detectDbType(dataSource) }
        val pending = FlywayKit.pendingMigrations(
            moduleName, dataSource, effectiveConfig(datasourceKey), dbType, moduleConfigCustomizers
        )
        if (pending.isEmpty()) {
            log.info("[dry-run] module [$moduleName] on data source [$datasourceKey] is up to date; nothing pending.")
        } else {
            log.warn("[dry-run] module [$moduleName] on data source [$datasourceKey] has ${pending.size} pending migration(s), NOT applied: $pending")
        }
    }

    /**
     * Repair the schema history of one module (remove failed records, realign checksums — see
     * [FlywayKit.repair]); the data source is looked up from `datasource-config` like in
     * [migrateByModule]. Intended for ops after a hotfix edited an already-applied script.
     */
    fun repairByModule(moduleName: String) = repairByModule(moduleName, requireDatasourceKey(moduleName))

    /** 2-arg variant of [repairByModule] targeting an explicit data source key. */
    fun repairByModule(moduleName: String, datasourceKey: String) {
        val dataSource = resolveDataSource(moduleName, datasourceKey)
        val dbType = dbTypeCache.getOrPut(datasourceKey) { FlywayKit.detectDbType(dataSource) }
        FlywayKit.repair(moduleName, dataSource, effectiveConfig(datasourceKey), dbType, moduleConfigCustomizers)
    }

    /** Data source key of [moduleName] per `datasource-config`; error (with source tracing) when unmapped. */
    private fun requireDatasourceKey(moduleName: String): String =
        flywayMultiDatasourceProperties.getDataSourceKey(moduleName)
            ?: run {
                val sources = resolveDatasourceConfigSources().ifEmpty { listOf("unknown") }
                error("Module [$moduleName] has no data source configured under kudos.ability.flyway.datasource-config (configuration sources: $sources)")
            }

    /** Validate [datasourceKey] against the resolver's routing table and return its data source. */
    private fun resolveDataSource(moduleName: String, datasourceKey: String): javax.sql.DataSource {
        if (!dataSourceResolver.hasDataSource(datasourceKey)) {
            val sources = resolveDatasourceConfigSources().ifEmpty { listOf("unknown") }
            val errMsg = "The data source [$datasourceKey] configured for module [$moduleName] does not exist! All subsequent database updates are aborted! (configuration sources: $sources)"
            log.error(errMsg)
            error(errMsg)
        }
        return checkNotNull(dataSourceResolver.getDataSource(datasourceKey)) {
            "Data source [$datasourceKey] resolved to null"
        }
    }

    /**
     * Effective Flyway parameters for [datasourceKey]: the global `flyway-config` with the
     * matching `datasource-flyway-config` overrides merged on top (cached — the result is
     * immutable per startup).
     */
    private fun effectiveConfig(datasourceKey: String): FlywayConfig {
        val global = flywayMultiDatasourceProperties.flywayConfig
        return effectiveConfigCache.getOrPut(datasourceKey) {
            flywayMultiDatasourceProperties.datasourceFlywayConfig[datasourceKey]?.mergedInto(global) ?: global
        }
    }

    /**
     * Warn about `spring.flyway.*` keys found in the environment: this module never runs Spring
     * Boot's Flyway auto-configuration, so those keys have no effect — their kudos equivalents
     * live under `kudos.ability.flyway.flyway-config.*`. `spring.flyway.enabled` is exempt: this
     * module sets it to false itself, to keep Boot's auto-configuration inert should it ever
     * reappear on the classpath.
     *
     * @return the ignored property names (empty when the environment declares none)
     */
    internal fun warnOnIgnoredSpringFlywayKeys(): Set<String> {
        val ignored = linkedSetOf<String>()
        for (propertySource in environment.propertySources) {
            if (propertySource !is EnumerablePropertySource<*>) continue
            propertySource.propertyNames.filterTo(ignored) {
                it.startsWith("spring.flyway.") && it != "spring.flyway.enabled"
            }
        }
        if (ignored.isNotEmpty()) {
            log.warn(
                "kudos-ability-data-rdb-flyway ignores these spring.flyway.* properties: $ignored." +
                    " This module does not use Spring Boot's Flyway auto-configuration;" +
                    " move them to kudos.ability.flyway.flyway-config.* (see the module README)."
            )
        }
        return ignored
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
     * automatically handles both jar and filesystem protocols. Only directories qualify as module
     * names — stray files directly under `sql/` are ignored in both branches.
     */
    private fun listChildFolders(url: java.net.URL, sqlRootPath: String): List<String> {
        return if (url.protocol == "jar") {
            val paths = FileKit.listFilesOrDirsInJar(url.toString().removeSuffix(sqlRootPath), sqlRootPath)
            paths.filter { it.endsWith("/") }.map { it.removePrefix("$sqlRootPath/").removeSuffix("/") }
        } else {
            // url.path is percent-encoded (spaces, CJK characters, ...); go through the URI so the path is decoded
            val dir = runCatching { File(url.toURI()) }.getOrElse { File(url.path) }
            dir.listFiles()?.filter { it.isDirectory }?.map { it.name }.orEmpty()
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
