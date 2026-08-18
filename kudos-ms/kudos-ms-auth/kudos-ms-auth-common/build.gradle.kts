dependencies {
    api(project(":kudos-ms:kudos-ms-user:kudos-ms-user-common"))
    // auth 的 API 契约直接使用 sys 的 VO（SysResourceCacheEntry / MenuTreeNode），显式声明而非依赖 user-common 传递
    api(project(":kudos-ms:kudos-ms-sys:kudos-ms-sys-common"))
    // Used to attach method-level @GetMapping/@PostMapping on IAuthXxxApi so the Feign proxy can recognize them.
    // Same pattern as kudos-ms-sys-common / kudos-ms-user-common: method-level only, no @RequestMapping on the interface type.
    compileOnly(platform(libs.spring.boot.bom))
    compileOnly("org.springframework:spring-web")

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.boot.starter.test)
}
