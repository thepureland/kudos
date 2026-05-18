dependencies {
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc"))
    api(libs.seata.spring.boot.starter)

    // SeataFeignXidAutoConfiguration 编译需要这两组类型，但只有 classpath 同时具备它们时才激活
    // （@ConditionalOnClass 守卫），所以挂 compileOnly，不向下游暴露 Feign / spring-web 依赖。
    compileOnly(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))
    compileOnly(libs.spring.boot.starter.web)

    testImplementation(project(":kudos-test:kudos-test-container"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))
    // setUp() 里通过 SpringApplication.run(Application1::class.java, ...) 起两个 sub-app，
    // 这两个 sub-app 用 @EnableDiscoveryClient + spring-cloud-alibaba-nacos-discovery 注册到 Nacos
    // —— 但只有真正起了 HTTP 端口的 Web 应用才会注册（注册数据要带 host+port）。没有 starter-web 时
    // 它们是普通 ApplicationContext，server.port 配了也没用，主应用做 Feign 调用就 "Load balancer
    // does not contain an instance for the service ms12"。给测试 classpath 补上 starter-web 后，
    // sub-app 才能起真实 servlet 容器 → 完成注册 → Feign 找得到实例。
    //
    // 顺带：starter-web 自带 spring-boot-web-server，所以也不再需要单独补 spring-boot-web-server
    // 来满足 Seata 2.5 的 ApplicationListener<WebServerInitializedEvent> 类型解析。
    testImplementation(libs.spring.boot.starter.web)
    testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))
    testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-discovery:kudos-ability-distributed-discovery-nacos"))
    testImplementation(libs.postgresql)
//    testImplementation("com.mysql:mysql-connector-j:9.3.0")
}