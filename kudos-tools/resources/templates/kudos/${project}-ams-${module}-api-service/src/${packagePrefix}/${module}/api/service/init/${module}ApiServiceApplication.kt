package ${packagePrefix}.${module}.api.service.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

<@generateClassComment module+"-api-service启动入口"/>
@EnableKudos
//region your codes 1
class ${module}ApiServiceApplication
//endregion your codes 1

fun main(args : Array<String>) {
    //region your codes 2

    //endregion your codes 2
    SpringApplication.run(${module}ApiServiceApplication::class.java, *args)
}