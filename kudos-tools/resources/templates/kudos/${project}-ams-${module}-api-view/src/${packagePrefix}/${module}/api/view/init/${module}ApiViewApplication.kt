package ${packagePrefix}.${module}.api.view.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

<@generateClassComment module+"-api-view启动入口"/>
@EnableKudos
//region your codes 1
class ${module}ApiViewApplication
//endregion your codes 1

fun main(args : Array<String>) {
    //region your codes 2

    //endregion your codes 2
    SpringApplication.run(${module}ApiViewApplication::class.java, *args)
}