dependencies {
    api(project(":kudos-ms:kudos-ms-user:kudos-ms-user-common"))
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-http"))

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(project(":kudos-test:kudos-test-container"))
    // 契约测试要起真实 servlet 容器，让 mock controller 用共享契约上的 @GetExchange / @PostExchange 提供路由。
    testImplementation(libs.spring.boot.starter.web)
}
