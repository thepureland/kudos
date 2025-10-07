dependencies {
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc"))
    api(libs.alibaba.seata)
    api(libs.alibaba.cloud.starter.seata) {
        exclude("io.seata", "seata-spring-boot-starter")
    }

    testImplementation(project(":kudos-test:kudos-test-container"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))
//    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))
    testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-discovery:kudos-ability-distributed-discovery-nacos"))
    testImplementation(libs.postgresql)
//    testImplementation("com.mysql:mysql-connector-j:9.3.0")
}