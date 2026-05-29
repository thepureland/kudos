plugins {
    alias(libs.plugins.ktor) // Brings the Ktor BOM; consistent with kudos-ability-web-ktor, no need to declare a version on test deps.
}

dependencies {
    api(project(":kudos-context"))
    api(libs.ktor.server.websockets)

    // Optional Redis-backed distributed broadcasting (see distributed/redis/). compileOnly so
    // single-instance deployments do not pull Spring Data Redis through this module; apps that
    // want cross-process broadcasting add Spring Data Redis themselves.
    compileOnly(libs.spring.boot.starter.data.redis)

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(libs.ktor.server.test.host)
}
