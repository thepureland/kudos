plugins {
    alias(libs.plugins.ktor) // Brings the Ktor BOM; consistent with kudos-ability-web-ktor, no need to declare a version on test deps.
}

dependencies {
    api(project(":kudos-context"))
    api(libs.ktor.server.websockets)

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(libs.ktor.server.test.host)
}
