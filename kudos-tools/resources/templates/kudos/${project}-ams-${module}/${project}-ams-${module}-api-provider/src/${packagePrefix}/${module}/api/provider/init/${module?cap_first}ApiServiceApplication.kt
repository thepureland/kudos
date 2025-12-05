package ${packagePrefix}.${module}.api.provider.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

<@generateClassComment module+"-api-provider启动入口"/>
@EnableKudos
//region your codes 1
class ${module?cap_first}ApiProviderApplication
//endregion your codes 1

fun main(args : Array<String>) {
    //region your codes 2

    //endregion your codes 2
    SpringApplication.run(${module?cap_first}ApiProviderApplication::class.java, *args)
}