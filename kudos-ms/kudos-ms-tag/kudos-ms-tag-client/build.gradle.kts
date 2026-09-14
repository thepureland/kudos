dependencies {
    api(project(":kudos-ms:kudos-ms-tag:kudos-ms-tag-common"))
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-http"))

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(libs.spring.boot.starter.web)
}
