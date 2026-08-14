package io.kudos.ability.data.rdb.flyway.kit

import java.nio.charset.Charset

/**
 * The Flyway parameters this module actually supports, owned by kudos instead of borrowed from
 * Spring Boot's `FlywayProperties`.
 *
 * Why not reuse Spring Boot's type: this module never runs Boot's Flyway auto-configuration (it
 * builds one Flyway instance per module), so only a subset of `spring.flyway.*` could ever take
 * effect. Binding the whole Boot type made unsupported keys look supported — silently doing
 * nothing. This class IS the contract: every field here reaches every per-module Flyway instance,
 * and anything absent from it is deliberately out of scope (use [IFlywayModuleConfigCustomizer]).
 *
 * Bound from yml under `kudos.ability.flyway.flyway-config.*`; per-data-source overrides live in
 * [io.kudos.ability.data.rdb.flyway.multidatasource.FlywayMultiDataSourceProperties.FlywayOverrides].
 * Defaults below are the kudos defaults, not Flyway's: they apply as-is when used outside Spring
 * (code generators / CLI tools).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
open class FlywayConfig {

    /** `migrate` needs a history table; baseline creates it when missing. Kudos default: true. */
    var baselineOnMigrate: Boolean = true

    /** Baseline version; scripts below it are not executed by migrate. */
    var baselineVersion: String = "0"

    /** Script charset name, resolved via [Charset.forName]. */
    var encoding: String = "UTF-8"

    /** Whether migrations may be applied out of order. Kudos default: false. */
    var outOfOrder: Boolean = false

    /** Whether to validate applied migrations against the scripts on migrate. Kudos default: false. */
    var validateOnMigrate: Boolean = false

    /**
     * Whether a missing `sql/<module>/<dbType>` directory fails instead of silently reporting
     * "up to date". Kudos default: true (Flyway's own default is false) — running against an
     * empty schema is worse than failing at startup.
     */
    var failOnMissingLocations: Boolean = true

    /**
     * Whether Flyway's `clean` (drops everything in the schema) is disabled. Kudos default: true;
     * there is no reason for an application to be able to wipe its own schema at runtime.
     */
    var cleanDisabled: Boolean = true

    /** Whether `${...}` placeholders in scripts are replaced. Kudos default: false. */
    var placeholderReplacement: Boolean = false

    /** Placeholder name → value; merged per key by per-data-source overrides. */
    var placeholders: MutableMap<String, String> = mutableMapOf()

    /** Placeholder opening token. */
    var placeholderPrefix: String = "\${"

    /** Placeholder closing token. */
    var placeholderSuffix: String = "}"

    /** Separator between placeholder name and its default value. */
    var placeholderSeparator: String = ":"

    /** [encoding] as a [Charset]; throws on an unknown charset name. */
    fun charset(): Charset = Charset.forName(encoding)

    /** Shallow copy — used when merging per-data-source overrides so the global instance is never mutated. */
    fun copy(): FlywayConfig = FlywayConfig().also { copy ->
        copy.baselineOnMigrate = baselineOnMigrate
        copy.baselineVersion = baselineVersion
        copy.encoding = encoding
        copy.outOfOrder = outOfOrder
        copy.validateOnMigrate = validateOnMigrate
        copy.failOnMissingLocations = failOnMissingLocations
        copy.cleanDisabled = cleanDisabled
        copy.placeholderReplacement = placeholderReplacement
        copy.placeholders = LinkedHashMap(placeholders)
        copy.placeholderPrefix = placeholderPrefix
        copy.placeholderSuffix = placeholderSuffix
        copy.placeholderSeparator = placeholderSeparator
    }
}
