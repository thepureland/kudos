dependencies {
    api(project(":kudos-base"))
    // Used to attach method-level @GetMapping/@PostMapping on IMsgXxxApi so Feign proxies can recognize them.
    // Same pattern as sys-common / user-common / auth-common: method-level only; do not put @RequestMapping on the interface type.
    compileOnly(platform(libs.spring.boot.bom))
    compileOnly("org.springframework:spring-web")
}
