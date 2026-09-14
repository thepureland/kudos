package io.kudos.ms.tag.api.public.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

/**
 * Auto-configuration entry point for the public tag API.
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.tag.api.public"])
open class TagApiPublicAutoConfiguration : IComponentInitializer {

    override fun getComponentName() = "kudos-ms-tag-api-public"
}
