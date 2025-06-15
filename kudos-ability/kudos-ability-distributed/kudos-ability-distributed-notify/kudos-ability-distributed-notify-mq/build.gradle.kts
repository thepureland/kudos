dependencies {
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-notify:kudos-ability-distributed-notify-common"))
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-common"))
    api("com.alibaba:fastjson:1.2.83")

    testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-rocketmq"))
    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))
    testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-discovery:kudos-ability-distributed-discovery-nacos"))
    testImplementation("org.postgresql:postgresql")
    testImplementation("org.flywaydb:flyway-database-postgresql:11.8.2") // flyway-core 8.2.1及以后的版本 单独包flyway-core 不再支持postgres
    testImplementation("org.springframework.boot:spring-boot-starter-web")
}