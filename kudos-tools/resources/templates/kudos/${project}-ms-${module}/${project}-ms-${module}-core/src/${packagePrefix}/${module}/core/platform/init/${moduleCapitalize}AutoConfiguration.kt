package ${packagePrefix}.${module}.core.platform.init

import io.kudos.ability.data.rdb.ktorm.init.KtormAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


<@generateClassComment "Auto-configuration class for the "+module+" atomic service."/>
@Configuration
@ComponentScan(basePackages = ["${packagePrefix}.${module}.core"])
@AutoConfigureAfter(KtormAutoConfiguration::class)
//region your codes 1
open class ${moduleCapitalize}AutoConfiguration : IComponentInitializer {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

    override fun getComponentName() = "${project}-ms-${module}-core"

}
