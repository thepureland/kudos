dependencies {
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-common"))
    api(libs.spring.cloud.starter.stream.kafka)

    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))
    testImplementation(libs.h2database.h2)
//    testImplementation("org.flywaydb:flyway-database-postgresql:11.8.2") // Since flyway-core 8.2.1, the standalone flyway-core jar no longer supports postgres
    testImplementation(libs.spring.boot.starter.web)
}