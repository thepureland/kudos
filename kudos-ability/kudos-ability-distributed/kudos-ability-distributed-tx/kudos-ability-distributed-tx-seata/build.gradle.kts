dependencies {
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc"))
    api(libs.seata.spring.boot.starter)

    // SeataFeignXidAutoConfiguration needs both type sets to compile, but it only activates when the
    // classpath has both at runtime (guarded by @ConditionalOnClass), so we use compileOnly to avoid
    // leaking Feign / spring-web dependencies downstream.
    compileOnly(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))
    compileOnly(libs.spring.boot.starter.web)

    testImplementation(project(":kudos-test:kudos-test-container"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))
    // setUp() launches two sub-apps via SpringApplication.run(Application1::class.java, ...).
    // These sub-apps register with Nacos through @EnableDiscoveryClient + spring-cloud-alibaba-nacos-discovery
    // — but only web apps that actually bind an HTTP port get registered (registration data needs host+port).
    // Without starter-web they are plain ApplicationContexts, configuring server.port has no effect, and the
    // main app's Feign calls fail with "Load balancer does not contain an instance for the service ms12".
    // Adding starter-web to the test classpath lets the sub-apps start a real servlet container -> register
    // -> Feign can locate the instances.
    //
    // Bonus: starter-web already brings in spring-boot-web-server, so we no longer need to add
    // spring-boot-web-server separately to satisfy Seata 2.5's ApplicationListener<WebServerInitializedEvent>
    // type resolution.
    testImplementation(libs.spring.boot.starter.web)
    testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))
    testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-discovery:kudos-ability-distributed-discovery-nacos"))
    testImplementation(libs.postgresql)
//    testImplementation("com.mysql:mysql-connector-j:9.3.0")
}