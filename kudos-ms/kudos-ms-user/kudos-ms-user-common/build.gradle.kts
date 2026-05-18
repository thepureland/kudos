dependencies {
    api(project(":kudos-ms:kudos-ms-sys:kudos-ms-sys-common"))
    // 用于在 IUserXxxApi 上挂方法级 @GetMapping/@PostMapping，让 Feign 代理可识别。
    // 与 kudos-ms-sys-common 同模式：仅方法级，不在接口类型上放 @RequestMapping。
    compileOnly(platform(libs.spring.boot.bom))
    compileOnly("org.springframework:spring-web")
}
