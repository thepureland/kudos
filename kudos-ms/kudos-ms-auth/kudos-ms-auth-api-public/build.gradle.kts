dependencies {
    api(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-core"))
    api(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-provider-oauth2"))
    api(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-provider-webauthn"))
    api(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-token-jwt"))
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-springmvc"))
    


    testImplementation(project(":kudos-test:kudos-test-container"))
}
