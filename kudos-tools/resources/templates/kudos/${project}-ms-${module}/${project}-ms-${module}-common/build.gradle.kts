dependencies {
<#if project == "kudos">
    api(project(":kudos-context"))
<#else>
    api("io.kudos:kudos-context")
</#if>
    compileOnly(platform(libs.spring.boot.bom))
    compileOnly("com.fasterxml.jackson.core:jackson-annotations")
    // 供 common 的 Api 接口使用方法级 @GetExchange / @PostExchange / @RequestParam / @RequestBody：
    // 同一份声明既是 interface client 的客户端契约，也是 Spring MVC 7 的服务端 request mapping。
    // 只能加在方法上；**不要**在接口类型上加 @RequestMapping 或 @HttpExchange——
    // 前者会让任何实现它的 @Component 被当成 Spring MVC handler，后者在 Spring MVC 7 里同样被
    // RequestMappingHandlerMapping.isHandler() 识别，还会让客户端代理 bean 被注册成 controller。
    compileOnly("org.springframework:spring-web")

    //region your codes 1

    //endregion your codes 1

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.boot.starter.test)
}
