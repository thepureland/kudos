dependencies {
    api(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-core"))
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-springmvc"))
    api(platform(libs.spring.boot.bom))
    api(libs.spring.security.oauth2.jose)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.bouncycastle.bcpkix)
}
