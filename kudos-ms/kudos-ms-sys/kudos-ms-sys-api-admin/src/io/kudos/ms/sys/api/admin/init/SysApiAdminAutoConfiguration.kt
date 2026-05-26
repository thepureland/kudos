package io.kudos.ms.sys.api.admin.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * sys-api-admin auto configuration class.
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.sys.api.admin"])
open class SysApiAdminAutoConfiguration : IComponentInitializer {



    override fun getComponentName() = "kudos-ms-sys-api-admin"

}