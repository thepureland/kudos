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
 * 本地磁盘文件存储装配入口。注册三个 SPI 实现（[LocalUploadService] /
 * [LocalDownLoadService] / [LocalDeleteService]）和配置 bean [LocalProperties]。
 *
 * 默认 `base-path = ${user.home}/fserver/upload`（见 `kudos-ability-file-local.yml`）；
 * 生产部署务必通过 yml 覆盖到独立的存储挂载点，否则与系统其他文件混在一起。
 *
 * @author K
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
