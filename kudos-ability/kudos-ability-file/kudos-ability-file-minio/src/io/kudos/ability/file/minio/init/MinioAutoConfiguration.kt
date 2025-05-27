package io.kudos.ability.file.minio.init

import io.kudos.context.spring.YamlPropertySourceFactory
import io.minio.MinioClient
import jakarta.annotation.PostConstruct
import org.soul.ability.file.minio.MinioDeleteService
import org.soul.ability.file.minio.MinioDownLoadService
import org.soul.ability.file.minio.MinioUploadService
import org.soul.ability.file.minio.client.MinioClientBuilderFactory
import org.soul.ability.file.minio.starter.MinioConfiguration
import org.soul.ability.file.minio.starter.properties.AccessTokenServerProperties
import org.soul.ability.file.minio.starter.properties.MinioProperties
import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import java.net.MalformedURLException
import java.net.URL

/**
 * minio自动配置类
 *
 * @author unknown
 * @author K
 * @since 1.0.0
 */
@Configuration
@PropertySource(value = ["classpath:kudos-ability-file-minio.yml"], factory = YamlPropertySourceFactory::class)
class MinioAutoConfiguration {
    @Bean
    @Throws(MalformedURLException::class)
    fun minioClient(minioProperties: MinioProperties): MinioClient {
        return MinioClient.builder().endpoint(URL(minioProperties.getEndpoint()))
            .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey()).build()
    }

    @Bean
    @ConfigurationProperties(prefix = "kudos.ability.file.minio")
    fun minioProperties(): MinioProperties {
        return MinioProperties()
    }

    @Bean
    @ConfigurationProperties(prefix = "kudos.ability.file.minio.sts.access-token")
    fun accessTokenProperties(): AccessTokenServerProperties {
        return AccessTokenServerProperties()
    }

    @Bean
    fun minioUploadService(): MinioUploadService {
        return MinioUploadService()
    }

    @Bean
    fun minioDownLoadService(): MinioDownLoadService {
        return MinioDownLoadService()
    }

    @Bean
    fun minioDeleteService(): MinioDeleteService {
        return MinioDeleteService()
    }

    @Bean
    fun minioClientBuilderFactory(): MinioClientBuilderFactory {
        return MinioClientBuilderFactory()
    }

    @PostConstruct
    fun init() {
        LOG.info("[soul-ability-file-minio]初始化完成...")
    }

    companion object {
        private val LOG: Log = LogFactory.getLog(MinioConfiguration::class.java)
    }
}
