dependencies {
    api(project(":kudos-context"))

    // spring-security-oauth2-jose drags in NimbusJwtEncoder / NimbusJwtDecoder + JWKSet /
    // RSAKey / JWSAlgorithm via nimbus-jose-jwt. Pulled as api so apps using JwtEncoder /
    // JwtDecoder don't need to redeclare the dep themselves.
    api(libs.spring.security.oauth2.jose)

    testImplementation(project(":kudos-test:kudos-test-common"))
    // Programmatically generates a self-signed X.509 + PKCS12 keystore at test setup so the
    // integration test exercises the real loadKeyPair() code path without committing a binary
    // .p12 blob to the repo.
    testImplementation(libs.bouncycastle.bcpkix)
}
