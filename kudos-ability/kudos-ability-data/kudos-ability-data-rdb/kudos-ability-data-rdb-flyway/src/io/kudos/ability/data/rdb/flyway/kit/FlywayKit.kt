package io.kudos.ability.data.rdb.flyway.kit

import io.kudos.ability.data.rdb.jdbc.kit.RdbKit
import io.kudos.base.logger.LogFactory
import org.flywaydb.core.Flyway
import javax.sql.DataSource


/**
 * Flyway utility (usable outside of a Spring container).
 *
 * Primarily intended for "non-Spring contexts" — e.g. code generators or standalone ops scripts —
 * where you can run Flyway against a [DataSource] using the same rules, without bootstrapping a
 * Spring context. Standard migrations inside a Spring container are performed via
 * [io.kudos.ability.data.rdb.flyway.multidatasource.FlywayMultiDataSourceMigrator] which delegates
 * to this utility.
 *
 * Convention: SQL scripts are organized under `classpath:sql/<moduleName>/<dbType>/V*.sql`
 * ([SQL_ROOT_PATH] is the root directory name), where dbType is the lowercase form of
 * `RdbTypeEnum::name` (h2 / postgresql / mysql, etc.). An optional `sql/<moduleName>/common/`
 * directory ([COMMON_DIR]) holds scripts shared by every database type; when present it is merged
 * into the same version stream (one history table per module — a version may exist in either
 * `common` or the dbType directory, not both).
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
object FlywayKit {

    /** Logger; any failure is rethrown to ensure schema mismatches surface at startup. */
    private val log = LogFactory.getLog(this::class)

    /** Classpath name of the SQL script root directory (conventionally "sql"). */
    const val SQL_ROOT_PATH = "sql"

    /** Name of the optional per-module subdirectory whose scripts apply to every database type. */
    const val COMMON_DIR = "common"

    /** Legal module name: letters/digits (unicode)/underscore/hyphen — it is embedded into the history table name and the classpath location. */
    private val MODULE_NAME_PATTERN = Regex("[\\p{L}\\p{N}_-]+")

    /**
     * Detect the database type of [dataSource] (lowercase `RdbTypeEnum#name`, e.g. h2 / postgresql).
     * Opens one connection; callers migrating several modules against the same data source should
     * detect once and pass the result to [migrate] via the `dbType` parameter.
     */
    fun detectDbType(dataSource: DataSource): String = dataSource.connection.use { conn ->
        RdbKit.determinRdbTypeByUrl(conn.metaData.url).name.lowercase()
    }

    /**
     * Upgrade the database schema of a single module.
     *
     * Behavior:
     * - Detects the database type of [dataSource] (unless [dbType] is given) and selects the
     *   `sql/<moduleName>/<dbType>` subdirectory, merged with `sql/<moduleName>/common` when that
     *   directory exists on the classpath.
     * - Each module uses its own `flyway_history_<moduleName>` metadata table, isolated from others.
     * - [config] controls Flyway common behavior such as baseline / encoding / outOfOrder;
     *   [customizers] run last and may override anything, including the locations and history table.
     *
     * Failure handling: **whenever Flyway reports success=false or throws any exception, this method
     * rethrows; callers should abort the overall startup process**. Treating "fail but continue" as
     * the default is dangerous (the app would run against a mismatched schema), so exceptions are
     * not swallowed here. With `failOnMissingLocations=true` (the kudos default) a missing
     * `sql/<moduleName>/<dbType>` directory also fails instead of silently reporting "up to date".
     *
     * @param moduleName module name (matches the direct subdirectory under SQL_ROOT_PATH)
     * @param dataSource data source
     * @param config the supported Flyway parameters (kudos defaults when omitted)
     * @param dbType pre-detected database type; `null` (default) detects from [dataSource]
     * @param customizers per-module configuration hooks, applied after the property mapping
     */
    fun migrate(
        moduleName: String,
        dataSource: DataSource,
        config: FlywayConfig = FlywayConfig(),
        dbType: String? = null,
        customizers: List<IFlywayModuleConfigCustomizer> = emptyList()
    ) {
        try {
            val flyway = buildFlyway(moduleName, dataSource, config, dbType, customizers)
            log.info(">>>>>>>>>>>>>  Starting database upgrade for module [$moduleName]...")
            val result = flyway.migrate()
            if (!result.success) {
                // Do not swallow — when Flyway reports failure, startup must be interrupted, otherwise we'd run against a mismatched schema
                error("flyway failed to upgrade the database for module [$moduleName]! warnings=${result.warnings}")
            }
            val migrationCount = result.migrationsExecuted
            if (migrationCount == 0) {
                log.info("<<<<<<<<<<<<<  Module [$moduleName] database is already up to date; no sql files were applied.")
            } else {
                log.info("<<<<<<<<<<<<<  Module [$moduleName] database upgrade completed; executed ${migrationCount} sql files, latest version: ${result.targetSchemaVersion}")
            }
        } catch (e: Exception) {
            log.error(e, "flyway failed while upgrading the database for module [$moduleName]!")
            throw e
        }
    }

    /**
     * Dry-run helper: return the migrations that WOULD be applied for [moduleName], without
     * touching the schema (`flyway.info()` only reads the history table if it exists — it never
     * creates or modifies anything). Each entry reads `"<version> - <description> (<script>)"`;
     * repeatable migrations use `R` as the version.
     */
    fun pendingMigrations(
        moduleName: String,
        dataSource: DataSource,
        config: FlywayConfig = FlywayConfig(),
        dbType: String? = null,
        customizers: List<IFlywayModuleConfigCustomizer> = emptyList()
    ): List<String> {
        try {
            val flyway = buildFlyway(moduleName, dataSource, config, dbType, customizers)
            return flyway.info().pending().map { info ->
                "${info.version?.toString() ?: "R"} - ${info.description} (${info.script})"
            }
        } catch (e: Exception) {
            log.error(e, "flyway failed to compute pending migrations for module [$moduleName]!")
            throw e
        }
    }

    /**
     * Repair the schema history table of a single module (Flyway `repair()`): removes records of
     * failed migrations and realigns checksums with the scripts currently on the classpath (the
     * typical fix after a hotfix edited an already-applied script). Never modifies business tables.
     */
    fun repair(
        moduleName: String,
        dataSource: DataSource,
        config: FlywayConfig = FlywayConfig(),
        dbType: String? = null,
        customizers: List<IFlywayModuleConfigCustomizer> = emptyList()
    ) {
        try {
            val flyway = buildFlyway(moduleName, dataSource, config, dbType, customizers)
            log.info(">>>>>>>>>>>>>  Starting schema history repair for module [$moduleName]...")
            val result = flyway.repair()
            log.info(
                "<<<<<<<<<<<<<  Module [$moduleName] schema history repair completed; " +
                    "removed ${result.migrationsRemoved.size} failed record(s), " +
                    "aligned ${result.migrationsAligned.size} checksum(s), " +
                    "marked ${result.migrationsDeleted.size} record(s) as deleted."
            )
        } catch (e: Exception) {
            log.error(e, "flyway failed to repair the schema history for module [$moduleName]!")
            throw e
        }
    }

    /**
     * Shared configuration for [migrate] / [pendingMigrations] / [repair]: validates the module
     * name, resolves locations (optional common dir + dbType dir), maps the supported
     * [FlywayProperties] subset, then lets [customizers] override anything before `load()`.
     */
    private fun buildFlyway(
        moduleName: String,
        dataSource: DataSource,
        config: FlywayConfig,
        dbType: String?,
        customizers: List<IFlywayModuleConfigCustomizer>
    ): Flyway {
        require(moduleName.matches(MODULE_NAME_PATTERN)) {
            "Illegal Flyway module name [$moduleName]: only letters, digits, '_' and '-' are allowed" +
                " (the name is embedded into the history table name and the classpath location)"
        }
        val actualDbType = dbType ?: detectDbType(dataSource)
        val locations = buildList {
            val commonPath = "$SQL_ROOT_PATH/$moduleName/$COMMON_DIR"
            // Only include the common dir when it actually exists — failOnMissingLocations would otherwise trip on it
            if (classpathDirExists(commonPath)) add("classpath:$commonPath")
            add("classpath:$SQL_ROOT_PATH/$moduleName/$actualDbType")
        }
        val configuration = Flyway.configure()
            .dataSource(dataSource)
            .table("flyway_history_$moduleName")
            .locations(*locations.toTypedArray())
            .baselineOnMigrate(config.baselineOnMigrate)
            .baselineVersion(config.baselineVersion)
            .encoding(config.charset())
            .outOfOrder(config.outOfOrder)
            .validateOnMigrate(config.validateOnMigrate)
            .failOnMissingLocations(config.failOnMissingLocations)
            .cleanDisabled(config.cleanDisabled)
            .placeholderReplacement(config.placeholderReplacement)
            .placeholders(config.placeholders)
            .placeholderPrefix(config.placeholderPrefix)
            .placeholderSuffix(config.placeholderSuffix)
            .placeholderSeparator(config.placeholderSeparator)
        customizers.forEach { it.customize(moduleName, configuration) }
        return configuration.load()
    }

    /** Whether a directory exists on the classpath under [path] (works for both filesystem and jar entries). */
    private fun classpathDirExists(path: String): Boolean {
        val classLoader = Thread.currentThread().contextClassLoader ?: FlywayKit::class.java.classLoader
        return classLoader.getResources(path).hasMoreElements()
    }
}
