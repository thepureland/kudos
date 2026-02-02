package ${packagePrefix}.${module}.api.web.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


<@generateClassComment module+"-api-web自动配置类"/>
@Configuration
@ComponentScan(basePackages = ["${packagePrefix}.${module}.api.view"])
//region your codes 1
open class ${module?cap_first}ApiWebAutoConfiguration : IComponentInitializer {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

    override fun getComponentName() = "kudos-ms-sys-api-public"

}