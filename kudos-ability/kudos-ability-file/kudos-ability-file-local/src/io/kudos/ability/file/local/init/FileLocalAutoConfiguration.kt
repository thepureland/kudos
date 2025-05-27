package io.kudos.ability.file.local.init

import jakarta.annotation.PostConstruct
import org.soul.ability.file.local.LocalDeleteService
import org.soul.ability.file.local.LocalDownLoadService
import org.soul.ability.file.local.LocalUploadService
import org.soul.ability.file.local.starter.FileLocalConfiguration
import org.soul.ability.file.local.starter.properties.LocalProperties
import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.soul.context.core.SoulPropertySourceFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration
@PropertySource(value = ["classpath:soul-ability-file-local.yml"], factory = SoulPropertySourceFactory::class)
class FileLocalAutoConfiguration {
    @Bean
    fun localUploadService(): LocalUploadService {
        return LocalUploadService()
    }

    @Bean
    fun localDownLoadService(): LocalDownLoadService {
        return LocalDownLoadService()
    }

    @Bean
    fun localDeleteService(): LocalDeleteService {
        return LocalDeleteService()
    }

    @Bean
    @ConfigurationProperties(prefix = "soul.ability.file.local")
    fun localProperties(): LocalProperties {
        return LocalProperties()
    }

    @PostConstruct
    fun init() {
        LOG.info("[soul-ability-file-local]初始化完成...")
    }

    companion object {
        private val LOG: Log = LogFactory.getLog(FileLocalConfiguration::class.java)
    }
}
