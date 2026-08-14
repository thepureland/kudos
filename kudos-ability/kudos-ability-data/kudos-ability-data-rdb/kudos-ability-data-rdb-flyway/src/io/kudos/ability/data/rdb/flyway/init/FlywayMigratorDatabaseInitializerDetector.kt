package io.kudos.ability.data.rdb.flyway.init

import io.kudos.ability.data.rdb.flyway.multidatasource.FlywayMultiDataSourceMigrator
import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDatabaseInitializerDetector

/**
 * Registers [FlywayMultiDataSourceMigrator] with Spring Boot's database-initialization dependency
 * mechanism ([org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer]).
 *
 * Effect: every bean detected as "depends on database initialization" (beans annotated with
 * [org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization], plus types
 * detected by Boot's built-in detectors such as JdbcTemplate/JdbcClient) automatically gets a
 * `dependsOn` edge to the migrator bean, so it is created only AFTER all module migrations have
 * completed. Without this, the "migrations run before any DB access" guarantee relied on
 * accidental bean ordering.
 *
 * Registered via `META-INF/spring.factories` under the
 * `org.springframework.boot.sql.init.dependency.DatabaseInitializerDetector` key; only takes
 * effect in contexts where Boot auto-configuration is active (kudos apps: `@EnableKudos` includes
 * `@EnableAutoConfiguration`). Beans not covered by any detector should declare
 * `@DependsOnDatabaseInitialization` themselves.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class FlywayMigratorDatabaseInitializerDetector : AbstractBeansOfTypeDatabaseInitializerDetector() {

    override fun getDatabaseInitializerBeanTypes(): Set<Class<*>> =
        setOf(FlywayMultiDataSourceMigrator::class.java)
}
