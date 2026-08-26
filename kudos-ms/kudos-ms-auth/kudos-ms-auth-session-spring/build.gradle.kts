dependencies {
    api(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-core"))
    // spring-session-core only: which store backs it stays the deployment's choice, exactly as it is for the
    // rest of this repository.
    api(libs.spring.session.core)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.boot.starter.test)
}
