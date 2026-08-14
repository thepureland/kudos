package io.kudos.ability.data.rdb.flyway.kit

import org.flywaydb.core.api.configuration.FluentConfiguration

/**
 * Extension point: customize the per-module Flyway [FluentConfiguration] before `load()`.
 *
 * [FlywayKit] only propagates the common subset of `spring.flyway.*` (baseline / encoding /
 * outOfOrder / validate / placeholder* / failOnMissingLocations); everything else — callbacks,
 * javaMigrations, extra locations, per-module schemas, ... — goes through this interface.
 * Register beans of this type and [FlywayMultiDataSourceMigrator] applies them (in bean order)
 * to every module's configuration, AFTER the standard property mapping, so customizers can
 * override anything. Use [moduleName] to scope the customization to specific modules.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
fun interface IFlywayModuleConfigCustomizer {

    /**
     * Customize the Flyway configuration of one module.
     *
     * @param moduleName the module about to be migrated
     * @param configuration the fully property-mapped configuration, not yet `load()`-ed
     */
    fun customize(moduleName: String, configuration: FluentConfiguration)
}
