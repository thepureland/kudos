package io.kudos.ability.web.swagger.init

import io.kudos.ability.web.swagger.init.properties.SwaggerProperties
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.IComponentInitializer
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

/**
 * Auto-configuration that publishes a single [OpenAPI] bean populated from [SwaggerProperties].
 *
 * Gating rules:
 *  - [ConditionalOnClass] on [OpenAPI]: only activates when springdoc is actually on the classpath
 *    (so depending on `kudos-context` indirectly does not force-wire an OpenAPI bean).
 *  - [ConditionalOnProperty] `kudos.ability.web.swagger.enabled` (default `true`): apps that pull
 *    the module in but want it off in a profile flip the flag without excluding the dependency.
 *  - [ConditionalOnMissingBean] on [openAPI]: apps needing a richer document (security schemes,
 *    external docs, multiple servers) can declare their own [OpenAPI] bean and this default
 *    steps aside.
 *
 * Ported from soul's `Swagger3Configuration`. Soul additionally registered a `SoulOpenAPIService`
 * that extended springdoc's [org.springdoc.core.service.OpenAPIService] purely to wire knife4j's
 * `@ApiSupport(order=N)` annotation into OpenAPI tag extensions. That extension is intentionally
 * **not** ported until a downstream kudos app actually adopts knife4j — keeping the dep surface to
 * springdoc-only avoids forcing knife4j on every consumer.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(OpenAPI::class)
@ConditionalOnProperty(
    prefix = "kudos.ability.web.swagger",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(SwaggerProperties::class)
@PropertySource(
    value = ["classpath:kudos-ability-web-swagger.yml"],
    factory = YamlPropertySourceFactory::class,
)
open class SwaggerAutoConfiguration(
    private val properties: SwaggerProperties,
) : IComponentInitializer {

    @Bean
    @ConditionalOnMissingBean
    open fun openAPI(): OpenAPI = OpenAPI().info(buildInfo())

    private fun buildInfo(): Info {
        val contact = Contact().apply {
            name = properties.contact.contactName
            url = properties.contact.contactUrl
            email = properties.contact.contactEmail
        }
        return Info()
            .title(properties.title)
            .description(properties.description)
            .version(properties.version)
            .contact(contact)
    }

    override fun getComponentName() = "kudos-ability-web-swagger3"
}
