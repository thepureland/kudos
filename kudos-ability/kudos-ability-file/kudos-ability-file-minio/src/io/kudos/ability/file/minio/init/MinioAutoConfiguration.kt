package io.kudos.ability.file.minio.init

import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import io.kudos.context.spring.YamlPropertySourceFactory
import io.minio.MinioClient
import org.soul.ability.file.minio.MinioDeleteService
import org.soul.ability.file.minio.MinioDownLoadService
import org.soul.ability.file.minio.MinioUploadService
import org.soul.ability.file.minio.client.MinioClientBuilderFactory
import org.soul.ability.file.minio.starter.properties.AccessTokenServerProperties
import org.soul.ability.file.minio.starter.properties.MinioProperties
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import java.net.URL

/**
 * minio自动配置类
 *
 * @author unknown
 * @author K
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
        return MinioClient.builder().endpoint(URL(minioProperties.endpoint))
            .credentials(minioProperties.accessKey, minioProperties.secretKey).build()
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

    override fun getComponentName() = "soul-ability-file-minio"

}
