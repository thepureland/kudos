dependencies {
    api(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-core"))

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation(libs.spring.boot.starter.data.redis)
}
