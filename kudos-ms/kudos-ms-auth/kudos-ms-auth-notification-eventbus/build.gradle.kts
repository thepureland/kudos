dependencies {
    api(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-core"))
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-common"))

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.boot.starter.test)
}
