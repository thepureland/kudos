dependencies {
    api(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-core"))
    api(platform(libs.spring.boot.bom))
    implementation(libs.yubico.webauthn.server.core)
    implementation(libs.yubico.webauthn.server.attestation)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation(libs.spring.boot.starter.data.redis)
    testImplementation("com.upokecenter:cbor:4.5.6")
}
