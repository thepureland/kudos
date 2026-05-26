dependencies {
    api(project(":kudos-ms:kudos-ms-user:kudos-ms-user-common"))
    // Used to attach method-level @GetMapping/@PostMapping on IAuthXxxApi so the Feign proxy can recognize them.
    // Same pattern as kudos-ms-sys-common / kudos-ms-user-common: method-level only, no @RequestMapping on the interface type.
    compileOnly(platform(libs.spring.boot.bom))
    compileOnly("org.springframework:spring-web")
}
