package io.kudos.ability.file.local.init

import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.soul.ability.file.local.LocalDeleteService
import org.soul.ability.file.local.LocalDownLoadService
import org.soul.ability.file.local.LocalUploadService
import org.soul.ability.file.local.starter.properties.LocalProperties
import org.soul.context.core.SoulPropertySourceFactory
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource


@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
@PropertySource(
    value = ["classpath:soul-ability-file-local.yml"],
    factory = SoulPropertySourceFactory::class
)
open class FileLocalAutoConfiguration : IComponentInitializer {

    @Bean
    open fun localUploadService() = LocalUploadService()

    @Bean
    open fun localDownLoadService() = LocalDownLoadService()

    @Bean
    open fun localDeleteService() = LocalDeleteService()

    @Bean
    @ConfigurationProperties(prefix = "soul.ability.file.local")
    open fun localProperties() = LocalProperties()

    override fun getComponentName() = "soul-ability-file-local"

}
