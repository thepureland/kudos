package io.kudos.ms.user.core.platform.init

import io.kudos.ability.data.rdb.ktorm.init.KtormAutoConfiguration
import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.data.memdb.redis.init.RedisAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import io.kudos.ms.user.core.passport.security.AuthenticationAttemptLimitProperties
import io.kudos.ms.user.core.passport.security.DefaultAuthenticationAttemptLimiter
import io.kudos.ms.user.core.passport.security.IAuthenticationAttemptLimiter
import io.kudos.ms.user.core.passport.security.IAuthenticationAttemptStore
import io.kudos.ms.user.core.passport.security.InMemoryAuthenticationAttemptStore
import io.kudos.ms.user.core.passport.security.RedisAuthenticationAttemptStore
import io.kudos.ms.user.core.account.security.DefaultPasswordPolicy
import io.kudos.ms.user.core.account.security.IPasswordPolicy
import io.kudos.ms.user.core.account.security.PasswordPolicyProperties
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * User atomic service auto-configuration class.
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.user.core"])
@AutoConfigureAfter(KtormAutoConfiguration::class, RedisAutoConfiguration::class)
@EnableConfigurationProperties(AuthenticationAttemptLimitProperties::class, PasswordPolicyProperties::class)
open class UserAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnBean(RedisTemplates::class)
    @ConditionalOnMissingBean(IAuthenticationAttemptStore::class)
    open fun redisAuthenticationAttemptStore(
        redisTemplates: RedisTemplates,
    ): IAuthenticationAttemptStore = RedisAuthenticationAttemptStore(redisTemplates)

    @Bean
    @ConditionalOnMissingBean(IAuthenticationAttemptStore::class)
    open fun authenticationAttemptStore(): IAuthenticationAttemptStore =
        InMemoryAuthenticationAttemptStore()

    @Bean
    @ConditionalOnMissingBean(IAuthenticationAttemptLimiter::class)
    open fun authenticationAttemptLimiter(
        store: IAuthenticationAttemptStore,
        properties: AuthenticationAttemptLimitProperties,
    ): IAuthenticationAttemptLimiter = DefaultAuthenticationAttemptLimiter(store, properties)

    @Bean
    @ConditionalOnMissingBean(IPasswordPolicy::class)
    open fun passwordPolicy(properties: PasswordPolicyProperties): IPasswordPolicy =
        DefaultPasswordPolicy(properties)

    override fun getComponentName() = "kudos-ms-user-core"

}
