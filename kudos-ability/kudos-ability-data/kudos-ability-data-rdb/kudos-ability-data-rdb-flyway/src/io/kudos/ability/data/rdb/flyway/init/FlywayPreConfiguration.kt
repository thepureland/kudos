package io.kudos.ability.data.rdb.flyway.init

import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Suppresses Spring Boot's default Flyway autoconfig by providing a bare [Flyway] bean.
 *
 * Spring Boot's `FlywayAutoConfiguration$FlywayConfiguration#flyway()` is `@ConditionalOnMissingBean(Flyway.class)`,
 * so registering our own — even an empty one — preempts it. Without this, Spring Boot would build
 * its own Flyway against the primary data source and try to migrate `classpath:db/migration`,
 * stepping on (or duplicating) our per-module migrations.
 *
 * Used together with [FlywayModuleStrategy], which neutralizes the default migration kick-off so
 * this bean truly stays inert.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "kudos.ability.flyway", name = ["enabled"], havingValue = "true", matchIfMissing = true)
open class FlywayPreConfiguration {

    /** Bare-bones Flyway bean — never invoked, exists only to win [@ConditionalOnMissingBean]. */
    @Bean
    open fun flyway(): Flyway = Flyway.configure().load()
}
