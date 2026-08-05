dependencies {
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-common"))

    api(libs.spring.boot.starter.web)
    // Kotlin data classes have no no-arg constructor, so without this module Jackson cannot build
    // one: every `@RequestBody` taking a data class fails with "no Creators, like default
    // constructor, exist". `api` rather than `implementation` because the request/response VOs that
    // need it live in the modules that depend on this one.
    api(libs.jackson.module.kotlin)
    api(libs.spring.session.core)
    api(libs.spring.session.data.redis)

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.jetty)
//    testImplementation(libs.spring.boot.starter.undertow)
}
