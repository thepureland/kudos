package ${packagePrefix}.${module}.client.${bizModule}.fallback

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport
import ${packagePrefix}.${module}.client.${bizModule}.proxy.I${entityName}Proxy
import ${packagePrefix}.${module}.common.${bizModule}.vo.${entityName}CacheEntry


<@generateClassComment (table.comment!table.name)+" client fallback implementation."/>
// 三条约束，漏了都不会在编译期报错：
//  1. 必须 open —— Spring Cloud 给 fallback 套 CGLIB 代理，Kotlin 类默认 final，
//     启动期会抛 AopConfigException。kotlin-spring allopen 插件只对 @Component / @Service
//     等注解生效，本类没有注解，救不了。
//  2. 不能加 @Component —— 本类实现 I${entityName}Proxy，若同时是 Spring bean，
//     按 I${entityName}Proxy 注入时会与 HTTP 代理 bean 产生歧义。
//     （Feign 时代没这个问题，因为 @FeignClient 注册的代理默认带 @Primary。）
//  3. 不能用构造注入 —— 框架走无参构造实例化。
// 绑定关系在 ${entityName}ClientConfiguration 的 @HttpServiceFallback 上。
//region your codes 1
open class ${entityName}Fallback : AbstractHttpFallbackSupport("${entityName}Fallback"), I${entityName}Proxy {
//endregion your codes 1

    override fun get(id: ${pkColumn.kotlinTypeName}): ${entityName}CacheEntry? {
        warnRead("get", id)
        return null
    }

    //region your codes 2

    //endregion your codes 2

}
