package ${packagePrefix}.${module}.client.${bizModule}.proxy

import ${packagePrefix}.${module}.common.${bizModule}.api.I${entityName}Api


<@generateClassComment (table.comment!table.name)+" client proxy interface."/>
// 接口上没有任何类型级注解，这是刻意的：
//  - 目标服务不写在接口上，由 ${moduleCapitalize}ClientAutoConfiguration 的 group
//    与部署配置的 base-url 决定；
//  - 尤其不能加类型级 @HttpExchange —— Spring MVC 7 的
//    RequestMappingHandlerMapping.isHandler() 认这个注解，加了会把本接口的客户端代理 bean
//    当成 controller 注册，与真正的 controller 撞路由（Ambiguous mapping），
//    或者把内部契约悄悄暴露成 HTTP 端点。
// 路径与 HTTP 方法全部继承自 I${entityName}Api 上的方法级 @GetExchange / @PostExchange。
//region your codes 1
interface I${entityName}Proxy : I${entityName}Api {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
