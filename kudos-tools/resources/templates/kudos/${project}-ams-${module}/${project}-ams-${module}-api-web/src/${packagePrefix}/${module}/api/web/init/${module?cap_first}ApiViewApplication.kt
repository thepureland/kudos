package ${packagePrefix}.${module}.api.web.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

<@generateClassComment module+"-api-web启动入口"/>
@EnableKudos
//region your codes 1
class ${module?cap_first}ApiWebApplication
//endregion your codes 1

fun main(args : Array<String>) {
    //region your codes 2

    //endregion your codes 2
    SpringApplication.run(${module?cap_first}ApiWebApplication::class.java, *args)
}