dependencies {
    api(project(":kudos-base"))
    // 用于在 IMsgXxxApi 上挂方法级 @GetMapping/@PostMapping，让 Feign 代理可识别。
    // 与 sys-common / user-common / auth-common 同模式：仅方法级，不在接口类型上放 @RequestMapping。
    compileOnly(platform(libs.spring.boot.bom))
    compileOnly("org.springframework:spring-web")
}
