package ${packagePrefix}.${module}.api.public.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication


<@generateClassComment module+"-api-public startup entry point."/>
@EnableKudos
//region your codes 1
class ${moduleCapitalize}ApiPublicApplication
//endregion your codes 1

fun main(args : Array<String>) {
    //region your codes 2

    //endregion your codes 2
    SpringApplication.run(${moduleCapitalize}ApiPublicApplication::class.java, *args)
}
