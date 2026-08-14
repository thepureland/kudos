dependencies {
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-common"))

    api(libs.spring.boot.starter.web)
    // Kotlin data classes have no no-arg constructor, so without this module Jackson cannot build
    // one: every `@RequestBody` taking a data class fails with "no Creators, like default
    // constructor, exist". `api` rather than `implementation` because the request/response VOs that
    // need it live in the modules that depend on this one.
    api(libs.jackson.module.kotlin)

    // `implementation`, not `api`: the only thing this module takes from Spring Session is the
    // `SessionRepositoryFilter.DEFAULT_ORDER` constant it orders its own filter against. Nothing in the public
    // surface exposes a Spring Session type, so nothing downstream needs it to compile.
    //
    // `spring-session-data-redis` used to sit here as `api` as well. It was removed: no code or configuration in
    // this repository ever selected a session store, so it forced the whole Redis-backed session stack onto every
    // application that depends on this module in exchange for nothing. An application that wants Redis-backed
    // sessions should declare that intent — and the store configuration that has to accompany it — for itself.
    implementation(libs.spring.session.core)

    // Jetty is expressed, not shipped. `SpringMvcAutoConfiguration.JettyConfiguration` needs the types at
    // compile time to declare its beans, but forcing Jetty onto every consumer of a Tomcat-based starter would
    // be wrong; `@ConditionalOnClass` keeps that configuration unloaded when Jetty is absent at runtime.
    compileOnly(libs.spring.boot.starter.jetty)

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.jetty)
//    testImplementation(libs.spring.boot.starter.undertow)
}
