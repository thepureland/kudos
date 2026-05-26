package io.kudos.ability.data.rdb.flyway.kit

import io.kudos.ability.data.rdb.jdbc.kit.RdbKit
import io.kudos.base.logger.LogFactory
import org.flywaydb.core.Flyway
import org.springframework.boot.flyway.autoconfigure.FlywayProperties
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
 * `RdbTypeEnum::name` (h2 / postgresql / mysql, etc.).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object FlywayKit {

    /** Logger; any failure is rethrown to ensure schema mismatches surface at startup. */
    private val log = LogFactory.getLog(this::class)

    /** Classpath name of the SQL script root directory (conventionally "sql"). */
    const val SQL_ROOT_PATH = "sql"

    /**
     * Upgrade the database schema of a single module.
     *
     * Behavior:
     * - Detects the database type of [dataSource] and selects the `sql/<moduleName>/<dbType>` subdirectory.
     * - Each module uses its own `flyway_history_<moduleName>` metadata table, isolated from others.
     * - [flywayProperties] controls Flyway common behavior such as baseline / encoding / outOfOrder.
     *
     * Failure handling: **whenever Flyway reports success=false or throws any exception, this method
     * rethrows; callers should abort the overall startup process**. Treating "fail but continue" as
     * the default is dangerous (the app would run against a mismatched schema), so exceptions are
     * not swallowed here.
     *
     * @param moduleName module name (matches the direct subdirectory under SQL_ROOT_PATH)
     * @param dataSource data source
     * @param flywayProperties reuses Spring Boot's [FlywayProperties] exposed to user configuration
     */
    fun migrate(moduleName: String, dataSource: DataSource, flywayProperties: FlywayProperties) {
        val dbType = dataSource.connection.use { conn ->
            RdbKit.determinRdbTypeByUrl(conn.metaData.url).name.lowercase()
        }
        try {
            val flyway = Flyway.configure()
                .dataSource(dataSource)
                .table("flyway_history_$moduleName")
                .locations("classpath:$SQL_ROOT_PATH/$moduleName/$dbType")
                .baselineOnMigrate(flywayProperties.isBaselineOnMigrate)
                .baselineVersion(flywayProperties.baselineVersion)
                .encoding(flywayProperties.encoding)
                .outOfOrder(flywayProperties.isOutOfOrder)
                .validateOnMigrate(flywayProperties.isValidateOnMigrate)
                .placeholderReplacement(flywayProperties.isPlaceholderReplacement)
                .placeholders(flywayProperties.placeholders)
                .placeholderPrefix(flywayProperties.placeholderPrefix)
                .placeholderSuffix(flywayProperties.placeholderSuffix)
                .placeholderSeparator(flywayProperties.placeholderSeparator)
                .load()
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
}
