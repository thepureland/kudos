dependencies {
    api(project(":kudos-ms:kudos-ms-sys:kudos-ms-sys-common"))
    // Used to attach method-level @GetMapping/@PostMapping on IUserXxxApi so Feign proxies can recognize them.
    // Same pattern as kudos-ms-sys-common: method-level only, no @RequestMapping on the interface type.
    compileOnly(platform(libs.spring.boot.bom))
    compileOnly("org.springframework:spring-web")
}
