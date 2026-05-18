dependencies {
    api(project(":kudos-ms:kudos-ms-user:kudos-ms-user-common"))
    // 用于在 IAuthXxxApi 上挂方法级 @GetMapping/@PostMapping，让 Feign 代理可识别。
    // 与 kudos-ms-sys-common / kudos-ms-user-common 同模式：仅方法级，不在接口类型上放 @RequestMapping。
    compileOnly(platform(libs.spring.boot.bom))
    compileOnly("org.springframework:spring-web")
}
