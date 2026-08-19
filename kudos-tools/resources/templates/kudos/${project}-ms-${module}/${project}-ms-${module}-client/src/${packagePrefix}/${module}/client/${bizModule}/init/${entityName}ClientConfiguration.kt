package ${packagePrefix}.${module}.client.${bizModule}.init

import ${packagePrefix}.${module}.client.${bizModule}.fallback.${entityName}Fallback
import ${packagePrefix}.${module}.client.${bizModule}.proxy.I${entityName}Proxy
import ${packagePrefix}.${module}.client.platform.init.${moduleCapitalize}ClientAutoConfiguration
import org.springframework.cloud.client.circuitbreaker.httpservice.HttpServiceFallback
import org.springframework.context.annotation.Configuration
import org.springframework.web.service.registry.ImportHttpServices


<@generateClassComment "Interface-client registration for "+(table.comment!table.name)+"."/>
// 每个实体自带一份注册，不需要在模块级维护聚合列表：同名 group 的多个 @ImportHttpServices
// 会合并成同一个 group。由 ${moduleCapitalize}ClientAutoConfiguration 上的 @ComponentScan 发现。
//region your codes 1
@Configuration
@ImportHttpServices(
    group = ${moduleCapitalize}ClientAutoConfiguration.GROUP,
    types = [I${entityName}Proxy::class]
)
@HttpServiceFallback(
    value = ${entityName}Fallback::class,
    service = [I${entityName}Proxy::class],
    group = ${moduleCapitalize}ClientAutoConfiguration.GROUP
)
open class ${entityName}ClientConfiguration
//endregion your codes 1
