package io.kudos.ms.auth.provider.emailotp.init

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.data.memdb.redis.init.RedisAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import io.kudos.ms.auth.provider.emailotp.EmailOtpAuthenticationMethodProvider
import io.kudos.ms.auth.provider.emailotp.EmailOtpProperties
import io.kudos.ms.auth.provider.emailotp.delivery.IEmailOtpDelivery
import io.kudos.ms.auth.provider.emailotp.identity.IEmailOtpPrincipalService
import io.kudos.ms.auth.provider.emailotp.identity.KudosEmailOtpPrincipalService
import io.kudos.ms.auth.provider.emailotp.store.IEmailOtpChallengeStore
import io.kudos.ms.auth.provider.emailotp.store.InMemoryEmailOtpChallengeStore
import io.kudos.ms.auth.provider.emailotp.store.RedisEmailOtpChallengeStore
import io.kudos.ms.user.core.passport.security.IAuthenticationAttemptLimiter
import io.kudos.ms.user.core.passport.security.NoopAuthenticationAttemptLimiter
import io.kudos.ms.user.core.account.service.iservice.IExternalAccountProvisioningService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdService
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(EmailOtpProperties::class)
@AutoConfigureAfter(RedisAutoConfiguration::class)
@ConditionalOnProperty(prefix = "kudos.ms.auth.email-otp", name = ["enabled"], havingValue = "true")
open class AuthProviderEmailOtpAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnBean(RedisTemplates::class)
    @ConditionalOnMissingBean(IEmailOtpChallengeStore::class)
    open fun redisEmailOtpChallengeStore(redisTemplates: RedisTemplates): IEmailOtpChallengeStore =
        RedisEmailOtpChallengeStore(redisTemplates)

    @Bean
    @ConditionalOnMissingBean(IEmailOtpChallengeStore::class)
    open fun emailOtpChallengeStore(): IEmailOtpChallengeStore = InMemoryEmailOtpChallengeStore()

    @Bean
    @ConditionalOnBean(
        IExternalAccountProvisioningService::class,
        IUserAccountService::class,
        IUserAccountThirdService::class,
    )
    @ConditionalOnMissingBean(IEmailOtpPrincipalService::class)
    open fun kudosEmailOtpPrincipalService(
        thirdService: IUserAccountThirdService,
        provisioningService: IExternalAccountProvisioningService,
        accountService: IUserAccountService,
        properties: EmailOtpProperties,
    ): IEmailOtpPrincipalService = KudosEmailOtpPrincipalService(
        thirdService,
        provisioningService,
        accountService,
        properties,
    )

    @Bean
    @ConditionalOnBean(IEmailOtpDelivery::class, IEmailOtpPrincipalService::class)
    @ConditionalOnMissingBean(EmailOtpAuthenticationMethodProvider::class)
    open fun emailOtpAuthenticationMethodProvider(
        store: IEmailOtpChallengeStore,
        delivery: IEmailOtpDelivery,
        principalService: IEmailOtpPrincipalService,
        properties: EmailOtpProperties,
        limiterProvider: ObjectProvider<IAuthenticationAttemptLimiter>,
    ) = EmailOtpAuthenticationMethodProvider(
        store = store,
        delivery = delivery,
        principalService = principalService,
        properties = properties,
        attemptLimiter = limiterProvider.ifAvailable ?: NoopAuthenticationAttemptLimiter,
    )

    override fun getComponentName() = "kudos-ms-auth-provider-email-otp"
}
