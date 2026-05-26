package io.kudos.ability.data.rdb.flyway.multidatasource

import io.kudos.ability.data.rdb.flyway.kit.FlywayKit
import io.kudos.ability.data.rdb.jdbc.datasource.DsContextProcessor
import io.kudos.base.io.FileKit
import io.kudos.base.io.scanner.classpath.ClassPathScanner
import io.kudos.base.logger.LogFactory
import jakarta.annotation.Resource
import org.springframework.boot.flyway.autoconfigure.FlywayProperties
import java.io.File


/**
 * Multi-data-source Flyway script upgrader.
 *
 * Conventions / behavior:
 * 1. SQL scripts are organized on the classpath by "module name + database type", e.g.
 *    `sql/<moduleName>/<dbType>/V1.0.1__xxx.sql`, where `<dbType>` is the lowercase form of
 *    `io.kudos.ability.data.rdb.jdbc.consts.RdbTypeEnum#name`.
 * 2. SQL script filenames follow the Flyway convention (V_x__xxx.sql / R__xxx.sql ...).
 * 3. Module upgrade order is determined by the declaration order of `kudos.ability.flyway.datasource-config`.
 * 4. If at startup a module name is found on the classpath more than once (e.g. spread across
 *    multiple jars), startup aborts with an error.
 * 5. If any module's migration fails, the remaining modules' upgrades are interrupted (the
 *    exception thrown by [FlywayKit] propagates).
 * 6. Supports upgrading multiple relational databases of different types in one pass (dbType is
 *    detected from each module's data source metadata).
 *
 * Coupling point: concrete data source instances are obtained via [DsContextProcessor], so this
 * currently **depends on the dynamic-routing data source provided by the baomidou
 * dynamic-datasource starter**.
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

    private val log = LogFactory.getLog(this::class)

    /**
     * Main entry point: scan the classpath, reconcile with the modules declared in properties,
     * and upgrade modules one by one in order. Any module failure propagates (no swallow), so
     * Spring startup is interrupted.
     */
    fun migrate() {
        val moduleNamesOnDisk = scanModuleNamesFromClasspath()
        val configuredModules = flywayMultiDatasourceProperties.datasourceConfig.sequencedKeySet()

        val orphanModules = configuredModules - moduleNamesOnDisk
        if (orphanModules.isNotEmpty()) {
            log.warn("The following modules configured in kudos.ability.flyway.datasource-config do not actually exist under the sql directory: $orphanModules")
        }

        val toMigrate = configuredModules - orphanModules
        toMigrate.forEach { module ->
            val datasourceKey = checkNotNull(flywayMultiDatasourceProperties.getDataSourceKey(module)) {
                "datasource key missing for module: $module"
            }
            migrateByModule(module, datasourceKey)
        }
    }

    /**
     * Scan direct subdirectories of `sql/` on the classpath and filter to module names that are
     * both on disk and in properties.
     *
     * Duplicate-detection semantics: if a module of the same name appears under multiple classpath
     * URLs (different jars / different source sets), an [IllegalStateException] is thrown. Flyway
     * does not support a single schema history table sourced from two different origins, so this
     * is treated as a fatal error to avoid confused migrations at runtime.
     */
    private fun scanModuleNamesFromClasspath(): Set<String> {
        val sqlRootPath = FlywayKit.SQL_ROOT_PATH
        val locationUrls = ClassPathScanner.getLocationUrlsForPath(sqlRootPath)
        val moduleNames = mutableSetOf<String>()
        locationUrls.forEach { url ->
            val childFolders = listChildFolders(url, sqlRootPath)
            childFolders.forEach { moduleName ->
                if (moduleNames.contains(moduleName)) {
                    error("Multiple modules named [$moduleName] exist! Please check whether the classpath contains duplicate sql/$moduleName directories.")
                }
                val datasourceKey = flywayMultiDatasourceProperties.getDataSourceKey(moduleName)
                if (!datasourceKey.isNullOrBlank()) {
                    moduleNames.add(moduleName)
                } else {
                    log.warn("No data source configured for module [$moduleName]! Ignored. Module location: ${url.path}/$moduleName")
                }
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
     * Single-module upgrade. First verifies that the data source key really exists in the dynamic
     * routing table, then delegates to [FlywayKit]. If the data source does not exist an
     * [IllegalStateException] is thrown; the caller decides whether to abort/retry.
     */
    internal fun migrateByModule(moduleName: String, datasourceKey: String) {
        if (!dsContextProcessor.haveDataSource(datasourceKey)) {
            val errMsg = "The data source [$datasourceKey] configured for module [$moduleName] does not exist! All subsequent database updates are aborted!"
            log.error(errMsg)
            error(errMsg)
        }

        val dataSource = checkNotNull(dsContextProcessor.getDataSource(datasourceKey)) {
            "Data source [$datasourceKey] resolved to null"
        }
        FlywayKit.migrate(moduleName, dataSource, flywayProperties)
    }
}
