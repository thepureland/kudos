package io.kudos.ms.auth.token.jwt.init

import io.kudos.context.init.IComponentInitializer
import io.kudos.ms.auth.common.authz.api.IPermissionVersionApi
import io.kudos.ms.auth.core.authentication.session.service.iservice.IAuthenticationSessionService
import io.kudos.ms.auth.core.token.refresh.service.iservice.IRefreshTokenService
import io.kudos.ms.auth.token.jwt.JwtTokenProperties
import io.kudos.ms.auth.token.jwt.model.IJwtAccessTokenService
import io.kudos.ms.auth.token.jwt.service.JwtAccessTokenService
import io.kudos.ms.auth.token.jwt.web.JwtBearerAuthenticationFilter
import io.kudos.ms.auth.token.jwt.web.JwtTokenPublicController
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder

/**
 * Enables JWT only when explicitly requested. JwtEncoder/JwtDecoder are mandatory deployment beans;
 * there is deliberately no ephemeral-key or weak shared-secret fallback.
 */
@Configuration
@EnableConfigurationProperties(JwtTokenProperties::class)
@ConditionalOnProperty(prefix = "kudos.ms.auth.token.jwt", name = ["enabled"], havingValue = "true")
open class AuthTokenJwtAutoConfiguration : IComponentInitializer {

    @Bean
    open fun jwtAccessTokenService(
        encoder: JwtEncoder,
        decoder: JwtDecoder,
        sessionService: IAuthenticationSessionService,
        permissionVersionApi: IPermissionVersionApi,
        properties: JwtTokenProperties,
    ): IJwtAccessTokenService = JwtAccessTokenService(
        encoder,
        decoder,
        sessionService,
        permissionVersionApi,
        properties,
    )

    @Bean
    open fun jwtTokenPublicController(
        accessTokenService: IJwtAccessTokenService,
        refreshTokenService: IRefreshTokenService,
        sessionService: IAuthenticationSessionService,
    ) = JwtTokenPublicController(accessTokenService, refreshTokenService, sessionService)

    @Bean
    open fun jwtBearerAuthenticationFilter(
        accessTokenService: IJwtAccessTokenService,
    ) = JwtBearerAuthenticationFilter(accessTokenService)

    override fun getComponentName() = "kudos-ms-auth-token-jwt"
}
