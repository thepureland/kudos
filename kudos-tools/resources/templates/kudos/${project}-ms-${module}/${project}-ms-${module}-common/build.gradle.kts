dependencies {
<#if project == "kudos">
    api(project(":kudos-context"))
<#else>
    api("io.kudos:kudos-context")
</#if>
    compileOnly(platform(libs.spring.boot.bom))
    compileOnly("com.fasterxml.jackson.core:jackson-annotations")
    // Used to put method-level @GetMapping/@PostMapping/@RequestParam/@RequestBody on the common Api interfaces
    // so Feign proxies can recognize them.
    // Method-level only; do not put @RequestMapping on the interface type -- otherwise any @Component implementing
    // it would be treated as a Spring MVC handler.
    compileOnly("org.springframework:spring-web")

    //region your codes 1

    //endregion your codes 1

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.boot.starter.test)
}
