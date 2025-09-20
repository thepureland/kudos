package io.kudos.ability.file.local.init

import io.kudos.ability.file.local.init.properties.LocalProperties
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource


@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
@PropertySource(
    value = ["classpath:kudos-ability-file-local.yml"],
    factory = YamlPropertySourceFactory::class
)
open class FileLocalAutoConfiguration : IComponentInitializer {

    @Bean
    open fun localUploadService() = LocalUploadService()

    @Bean
    open fun localDownLoadService() = LocalDownLoadService()

    @Bean
    open fun localDeleteService() = LocalDeleteService()

    @Bean
    @ConfigurationProperties(prefix = "kudos.ability.file.local")
    open fun localProperties() = LocalProperties()

    override fun getComponentName() = "kudos-ability-file-local"

}
