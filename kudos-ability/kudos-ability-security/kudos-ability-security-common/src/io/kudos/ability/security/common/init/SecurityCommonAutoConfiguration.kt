package io.kudos.ability.security.common.init

import io.kudos.ability.security.common.init.properties.SecurityCommonProperties
import io.kudos.ability.security.common.support.Authenticator
import io.kudos.ability.security.common.support.TotpAuthenticator
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * Wires the cross-cutting security primitives that aren't tied to JWT / captcha / device.
 *
 * Two beans:
 *  - [PasswordEncoder]: a [DelegatingPasswordEncoder] with `bcrypt` as the encoding id. This is
 *    Spring Security's standard pattern: stored hashes get an `{bcrypt}` prefix at write time,
 *    so future algorithm rotations (argon2, scrypt) can read old hashes via the same encoder
 *    bean without a DB migration.
 *  - [Authenticator]: a [TotpAuthenticator] tuned from [SecurityCommonProperties.Totp].
 *
 * Both are `@ConditionalOnMissingBean` so apps that need different crypto choices (a stricter
 * `Argon2PasswordEncoder`, or their own `Authenticator` impl backed by Vault) can override
 * without excluding the module.
 *
 * Ported from soul's `SecurityCommonConfiguration` with these changes:
 *  - Drop `@Import(SecurityJwtConfiguration.class)`. JWT lives in `kudos-ability-security-jwt`,
 *    a separate sub-module. Apps that want JWT add that module to their build directly.
 *  - Drop the `PasswordTool` bean. kudos-base's `PasswordKit` already covers this with a cleaner
 *    API. Apps just use `PasswordKit.hash()` / `PasswordKit.matches()` statically.
 *  - Drop `@ComponentScan`. Explicit `@Bean` declarations only.
 *  - Drop field-injected `@Autowired` props. Use constructor params on `@Bean` methods.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SecurityCommonProperties::class)
@PropertySource(
    value = ["classpath:kudos-ability-security-common.yml"],
    factory = YamlPropertySourceFactory::class,
)
open class SecurityCommonAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnMissingBean
    open fun passwordEncoder(): PasswordEncoder {
        val idForEncode = "bcrypt"
        val encoders = mapOf<String, PasswordEncoder>(idForEncode to BCryptPasswordEncoder())
        return DelegatingPasswordEncoder(idForEncode, encoders)
    }

    @Bean
    @ConditionalOnMissingBean
    open fun authenticator(props: SecurityCommonProperties): Authenticator =
        TotpAuthenticator(windowSize = props.totp.windowSize)

    override fun getComponentName() = "kudos-ability-security-common"
}
