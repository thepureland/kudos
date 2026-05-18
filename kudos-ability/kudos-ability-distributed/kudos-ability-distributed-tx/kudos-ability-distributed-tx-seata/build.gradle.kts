dependencies {
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc"))
    api(libs.seata.spring.boot.starter)

    testImplementation(project(":kudos-test:kudos-test-container"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))
    // Seata 2.5 的 spring-boot-starter 在 GlobalTransactionScanner 上挂了一个
    // ApplicationListener<WebServerInitializedEvent>。SB4 把 WebServerInitializedEvent
    // 从 spring-boot.jar 拆到了独立的 spring-boot-web-server artifact，而本测试 webEnv=MOCK
    // 不引 starter-web，导致泛型解析时 ClassNotFoundException 让整个上下文起不来。
    // 仅在测试侧补这一个最小 artifact 即可（版本由 spring-boot BOM 管理）。
    testImplementation("org.springframework.boot:spring-boot-web-server")
    testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))
    testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-discovery:kudos-ability-distributed-discovery-nacos"))
    testImplementation(libs.postgresql)
//    testImplementation("com.mysql:mysql-connector-j:9.3.0")
}