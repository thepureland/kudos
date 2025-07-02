package ${packagePrefix}.${module}.api.service.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


<@generateClassComment module+"-api-service自动配置类"/>
@Configuration
@ComponentScan(basePackages = ["${packagePrefix}.${module}.api.service"])
//region your codes 1
open class ${module}ApiServiceAutoConfiguration : IComponentInitializer {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

    override fun getComponentName() = "kudos-ams-sys-api-service"

}