package io.kudos.ability.data.rdb.flyway.init

import org.flywaydb.core.Flyway
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy

/**
 * No-op [FlywayMigrationStrategy] to prevent Spring Boot's [org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer]
 * from calling `flyway.migrate()` on the bare-bones Flyway bean wired in [FlywayPreConfiguration].
 *
 * Our actual migrations are driven by [io.kudos.ability.data.rdb.flyway.multidatasource.FlywayMultiDataSourceMigrator],
 * which constructs per-module Flyway instances against the right data source. Leaving the default
 * strategy in place would either no-op against an empty `db/migration` or, worse, run against the
 * primary data source with confusing results.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class FlywayModuleStrategy : FlywayMigrationStrategy {

    override fun migrate(flyway: Flyway) {
        // intentionally empty — see class kdoc
    }
}
