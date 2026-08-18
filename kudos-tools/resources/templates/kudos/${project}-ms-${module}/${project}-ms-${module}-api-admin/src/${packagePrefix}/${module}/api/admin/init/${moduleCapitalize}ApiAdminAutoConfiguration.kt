package ${packagePrefix}.${module}.api.admin.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


<@generateClassComment module+"-api-admin auto configuration class."/>
@Configuration
@ComponentScan(basePackages = ["${packagePrefix}.${module}.api.admin"])
//region your codes 1
open class ${moduleCapitalize}ApiAdminAutoConfiguration : IComponentInitializer {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

    override fun getComponentName() = "${project}-ms-${module}-api-admin"

}
