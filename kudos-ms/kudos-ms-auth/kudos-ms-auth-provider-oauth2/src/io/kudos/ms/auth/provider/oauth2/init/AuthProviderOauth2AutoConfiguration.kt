package io.kudos.ms.auth.provider.oauth2.init

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.data.memdb.redis.init.RedisAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import io.kudos.ms.auth.core.authentication.service.iservice.IAuthenticationTransactionService
import io.kudos.ms.auth.core.authentication.session.service.iservice.IAuthenticationSessionService
import io.kudos.ms.auth.provider.oauth2.authorization.IExternalAuthorizationRequestStore
import io.kudos.ms.auth.provider.oauth2.authorization.InMemoryExternalAuthorizationRequestStore
import io.kudos.ms.auth.provider.oauth2.authorization.KudosOAuth2AuthorizationRequestRepository
import io.kudos.ms.auth.provider.oauth2.authorization.RedisExternalAuthorizationRequestStore
import io.kudos.ms.auth.provider.oauth2.state.IExternalLoginStateStore
import io.kudos.ms.auth.provider.oauth2.state.InMemoryExternalLoginStateStore
import io.kudos.ms.auth.provider.oauth2.state.RedisExternalLoginStateStore
import io.kudos.ms.auth.provider.oauth2.web.DiscardingOAuth2AuthorizedClientRepository
import io.kudos.ms.auth.provider.oauth2.web.ExternalLoginAuthenticationFailureHandler
import io.kudos.ms.auth.provider.oauth2.web.ExternalLoginAuthenticationSuccessHandler
import io.kudos.ms.auth.provider.oauth2.web.KudosOAuth2AuthorizationRequestResolver
import io.kudos.ms.auth.provider.oauth2.service.ExternalIdentityAuthenticationService
import io.kudos.ms.auth.provider.oauth2.service.ExternalIdentityBindingService
import io.kudos.ms.auth.provider.oauth2.registration.DynamicClientRegistrationRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository

@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.auth.provider.oauth2"])
@EnableConfigurationProperties(ExternalLoginProperties::class)
@AutoConfigureAfter(RedisAutoConfiguration::class)
open class AuthProviderOauth2AutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnBean(RedisTemplates::class)
    @ConditionalOnMissingBean(IExternalLoginStateStore::class)
    open fun redisExternalLoginStateStore(redisTemplates: RedisTemplates): IExternalLoginStateStore =
        RedisExternalLoginStateStore(redisTemplates)

    @Bean
    @ConditionalOnMissingBean(IExternalLoginStateStore::class)
    open fun externalLoginStateStore(): IExternalLoginStateStore = InMemoryExternalLoginStateStore()

    @Bean
    @ConditionalOnBean(RedisTemplates::class)
    @ConditionalOnMissingBean(IExternalAuthorizationRequestStore::class)
    open fun redisExternalAuthorizationRequestStore(
        redisTemplates: RedisTemplates,
    ): IExternalAuthorizationRequestStore = RedisExternalAuthorizationRequestStore(redisTemplates)

    @Bean
    @ConditionalOnMissingBean(IExternalAuthorizationRequestStore::class)
    open fun externalAuthorizationRequestStore(): IExternalAuthorizationRequestStore =
        InMemoryExternalAuthorizationRequestStore()

    @Bean("externalLoginAuthorizationRequestRepository")
    @ConditionalOnMissingBean(name = ["externalLoginAuthorizationRequestRepository"])
    open fun externalLoginAuthorizationRequestRepository(
        store: IExternalAuthorizationRequestStore,
        properties: ExternalLoginProperties,
    ): AuthorizationRequestRepository<OAuth2AuthorizationRequest> =
        KudosOAuth2AuthorizationRequestRepository(store, properties)

    @Bean
    open fun oauth2AuthorizationRequestResolver(
        clientRegistrationRepository: DynamicClientRegistrationRepository,
        transactionService: IAuthenticationTransactionService,
        stateStore: IExternalLoginStateStore,
    ) = KudosOAuth2AuthorizationRequestResolver(clientRegistrationRepository, transactionService, stateStore)

    @Bean
    open fun discardingOAuth2AuthorizedClientRepository(): OAuth2AuthorizedClientRepository =
        DiscardingOAuth2AuthorizedClientRepository()

    @Bean
    open fun externalLoginSecurityContextRepository(): SecurityContextRepository =
        HttpSessionSecurityContextRepository()

    @Bean
    open fun externalLoginSuccessHandler(
        stateStore: IExternalLoginStateStore,
        externalIdentityService: ExternalIdentityAuthenticationService,
        externalIdentityBindingService: ExternalIdentityBindingService,
        transactionService: IAuthenticationTransactionService,
        sessionService: IAuthenticationSessionService,
        properties: ExternalLoginProperties,
        @Qualifier("externalLoginSecurityContextRepository")
        securityContextRepository: SecurityContextRepository,
    ) = ExternalLoginAuthenticationSuccessHandler(
        stateStore,
        externalIdentityService,
        externalIdentityBindingService,
        transactionService,
        sessionService,
        properties,
        securityContextRepository,
    )

    @Bean
    open fun externalLoginFailureHandler(
        stateStore: IExternalLoginStateStore,
        transactionService: IAuthenticationTransactionService,
        properties: ExternalLoginProperties,
    ) = ExternalLoginAuthenticationFailureHandler(stateStore, transactionService, properties)

    @Bean
    @Order(50)
    @ConditionalOnBean(HttpSecurity::class)
    open fun externalLoginSecurityFilterChain(
        http: HttpSecurity,
        clientRegistrationRepository: DynamicClientRegistrationRepository,
        authorizationRequestResolver: KudosOAuth2AuthorizationRequestResolver,
        @Qualifier("externalLoginAuthorizationRequestRepository")
        authorizationRequestRepository: AuthorizationRequestRepository<OAuth2AuthorizationRequest>,
        @Qualifier("discardingOAuth2AuthorizedClientRepository")
        authorizedClientRepository: OAuth2AuthorizedClientRepository,
        @Qualifier("externalLoginSecurityContextRepository")
        securityContextRepository: SecurityContextRepository,
        successHandler: ExternalLoginAuthenticationSuccessHandler,
        failureHandler: ExternalLoginAuthenticationFailureHandler,
    ): SecurityFilterChain = http
        .securityMatcher("/oauth2/authorization/**", "/api/public/auth/external/**", "/api/auth/external/**")
        .authorizeHttpRequests { it.anyRequest().permitAll() }
        .requestCache { it.disable() }
        .csrf { it.ignoringRequestMatchers("/api/public/auth/external/*/callback") }
        .oauth2Login { login ->
            login.clientRegistrationRepository(clientRegistrationRepository)
            login.authorizedClientRepository(authorizedClientRepository)
            login.securityContextRepository(securityContextRepository)
            login.authorizationEndpoint {
                it.authorizationRequestResolver(authorizationRequestResolver)
                it.authorizationRequestRepository(authorizationRequestRepository)
            }
            login.redirectionEndpoint { it.baseUri("/api/public/auth/external/*/callback") }
            login.successHandler(successHandler)
            login.failureHandler(failureHandler)
        }
        .build()

    override fun getComponentName() = "kudos-ms-auth-provider-oauth2"
}
