dependencies {
    api(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-provider-oauth2"))
    api(platform(libs.spring.boot.bom))
    api(platform(libs.spring.cloud.bom))
    api(libs.spring.vault.core)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.boot.starter.test)
}
