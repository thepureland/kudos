package io.kudos.ability.file.minio.init

import io.kudos.ability.file.minio.MinioDeleteService
import io.kudos.ability.file.minio.MinioDownLoadService
import io.kudos.ability.file.minio.MinioUploadService
import io.kudos.ability.file.minio.client.MinioClientBuilderFactory
import io.kudos.ability.file.minio.init.properties.AccessTokenServerProperties
import io.kudos.ability.file.minio.init.properties.MinioProperties
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import io.minio.MinioClient
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import java.net.URI

/**
 * MinIO auto configuration class.
 *
 * @author unknown
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
@PropertySource(
    value = ["classpath:kudos-ability-file-minio.yml"],
    factory = YamlPropertySourceFactory::class
)
open class MinioAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnMissingBean
    open fun minioClient(minioProperties: MinioProperties): MinioClient {
        val accessKey = requireConfigured(minioProperties.accessKey, "$PROPERTY_PREFIX.access-key")
        val secretKey = requireConfigured(minioProperties.secretKey, "$PROPERTY_PREFIX.secret-key")
        val endpoint = requireConfigured(minioProperties.endpoint, "$PROPERTY_PREFIX.endpoint")
        return MinioClient.builder()
            .endpoint(URI(endpoint).toURL())
            .credentials(accessKey, secretKey)
            .build()
    }

    /**
     * Fails fast when a required MinIO property is missing or blank.
     *
     * No default credentials ship with this jar (the bundled yml deliberately leaves
     * access-key/secret-key unset), so a missing value here means the application forgot
     * to configure them - better to abort startup with a clear hint than to connect to
     * MinIO with empty credentials and surface an obscure SDK error.
     *
     * @param value the configured value, possibly null or blank
     * @param propertyName full property name, used in the error message
     * @return the non-blank value
     * @throws IllegalStateException when the value is null or blank
     */
    internal fun requireConfigured(value: String?, propertyName: String): String {
        check(!value.isNullOrBlank()) {
            "MinIO property '$propertyName' is missing or blank. " +
                    "It has no built-in default and must be configured by the application " +
                    "(e.g. in application.yml or via an environment variable)."
        }
        return value
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.file.minio")
    open fun minioProperties() = MinioProperties()

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.file.minio.sts.access-token")
    open fun accessTokenProperties() = AccessTokenServerProperties()

    @Bean
    @ConditionalOnMissingBean
    open fun minioUploadService() = MinioUploadService()

    @Bean
    @ConditionalOnMissingBean
    open fun minioDownLoadService() = MinioDownLoadService()

    @Bean
    @ConditionalOnMissingBean
    open fun minioDeleteService() = MinioDeleteService()

    @Bean
    @ConditionalOnMissingBean
    open fun minioClientBuilderFactory() = MinioClientBuilderFactory()

    override fun getComponentName() = "kudos-ability-file-minio"

    companion object {
        /** Configuration prefix for MinIO properties. */
        const val PROPERTY_PREFIX = "kudos.ability.file.minio"
    }

}
