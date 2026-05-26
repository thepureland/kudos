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


/**
 * Auto-configuration entry point for local disk file storage. Registers three SPI implementations
 * ([LocalUploadService] / [LocalDownLoadService] / [LocalDeleteService]) and the configuration bean
 * [LocalProperties].
 *
 * Default `base-path = ${user.home}/fserver/upload` (see `kudos-ability-file-local.yml`); for production
 * deployment, be sure to override via yml to a dedicated storage mount point, otherwise files will be
 * mixed in with other system files.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
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
