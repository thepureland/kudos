dependencies {
    api(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-provider-oauth2"))
    api(libs.azure.security.keyvault.secrets)
    implementation(libs.jackson.databind)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.boot.starter.test)
}
