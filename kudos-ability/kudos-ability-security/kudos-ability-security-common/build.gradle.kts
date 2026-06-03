dependencies {
    api(project(":kudos-context"))
    // kudos-base for GoogleAuthenticator (verify path). api so apps can also reach the underlying
    // helper directly without re-declaring the dep.
    api(project(":kudos-base"))

    // spring-security-crypto for PasswordEncoder / DelegatingPasswordEncoder / BCryptPasswordEncoder.
    // api because the registered PasswordEncoder bean is part of this module's public surface.
    api(libs.spring.security.crypto)

    // Base32 for TOTP secret encoding. kudos-base already transitively brings commons-codec, but
    // declaring it explicitly here documents the direct use.
    implementation(libs.apache.commons.codec)

    testImplementation(project(":kudos-test:kudos-test-common"))
}
