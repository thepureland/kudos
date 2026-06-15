dependencies {
    api(project(":kudos-ms:kudos-ms-user:kudos-ms-user-core"))
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-springmvc"))



    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.boot.starter.test)
}
