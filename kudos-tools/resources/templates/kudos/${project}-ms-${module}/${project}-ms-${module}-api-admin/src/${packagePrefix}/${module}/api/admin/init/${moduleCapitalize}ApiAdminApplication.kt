package ${packagePrefix}.${module}.api.admin.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication


<@generateClassComment module+"-api-admin startup entry point."/>
@EnableKudos
//region your codes 1
class ${moduleCapitalize}ApiAdminApplication
//endregion your codes 1

fun main(args : Array<String>) {
    //region your codes 2

    //endregion your codes 2
    SpringApplication.run(${moduleCapitalize}ApiAdminApplication::class.java, *args)
}
