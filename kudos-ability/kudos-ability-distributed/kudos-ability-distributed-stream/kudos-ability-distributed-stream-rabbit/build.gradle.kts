dependencies {
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-common"))
    api(libs.spring.cloud.starter.stream.rabbit)

    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))
    testImplementation(libs.h2database.h2)
//    testImplementation("org.flywaydb:flyway-database-postgresql:11.8.2") // flyway-core 8.2.1及以后的版本 单独包flyway-core 不再支持postgres
    testImplementation(libs.spring.boot.starter.web)
}