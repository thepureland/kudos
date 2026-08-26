package io.kudos.ms.auth.provider.webauthn.init

import com.yubico.webauthn.attestation.AttestationTrustSource
import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.data.memdb.redis.init.RedisAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import io.kudos.ms.auth.provider.webauthn.WebAuthnProviderProperties
import io.kudos.ms.auth.provider.webauthn.ceremony.IWebAuthnCeremonyStore
import io.kudos.ms.auth.provider.webauthn.ceremony.InMemoryWebAuthnCeremonyStore
import io.kudos.ms.auth.provider.webauthn.ceremony.RedisWebAuthnCeremonyStore
import io.kudos.ms.auth.provider.webauthn.protocol.attestation.ReloadingFidoMdsAttestationTrustSource
import io.kudos.ms.auth.provider.webauthn.protocol.attestation.YubicoFidoMetadataLoader
import io.kudos.ms.auth.provider.webauthn.protocol.attestation.validated
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.auth.provider.webauthn"])
@EnableConfigurationProperties(WebAuthnProviderProperties::class)
@AutoConfigureAfter(RedisAutoConfiguration::class)
open class AuthProviderWebAuthnAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnBean(RedisTemplates::class)
    @ConditionalOnMissingBean(IWebAuthnCeremonyStore::class)
    open fun redisWebAuthnCeremonyStore(redisTemplates: RedisTemplates): IWebAuthnCeremonyStore =
        RedisWebAuthnCeremonyStore(redisTemplates)

    @Bean
    @ConditionalOnMissingBean(IWebAuthnCeremonyStore::class)
    open fun webAuthnCeremonyStore(): IWebAuthnCeremonyStore = InMemoryWebAuthnCeremonyStore()

    @Bean
    @ConditionalOnProperty(
        prefix = "kudos.ms.auth.webauthn",
        name = ["enabled", "fido-mds.enabled"],
        havingValue = "true",
    )
    @ConditionalOnMissingBean(AttestationTrustSource::class)
    open fun fidoMdsAttestationTrustSource(
        properties: WebAuthnProviderProperties,
    ): ReloadingFidoMdsAttestationTrustSource {
        val configuration = properties.fidoMds.validated()
        return ReloadingFidoMdsAttestationTrustSource(
            loader = YubicoFidoMetadataLoader(configuration),
            refreshInterval = configuration.refreshInterval,
        )
    }

    override fun getComponentName() = "kudos-ms-auth-provider-webauthn"
}
