dependencies {
    api(project(":kudos-context"))
    // Spring Boot 4 的 interface client 自动装配（HttpServiceClientAutoConfiguration /
    // PropertiesRestClientHttpServiceGroupConfigurer），同时带来 spring-web 的 RestClient 与
    // HttpServiceProxyFactory。取代 spring-cloud-starter-openfeign。
    api(libs.spring.boot.restclient)
    // LoadBalancerRestClientHttpServiceGroupConfigurer：把 group 的 `lb://<service-id>` base-url
    // 改写成 http 并挂上 LoadBalancer 拦截器，等价于 @FeignClient(name=...) 的服务发现。
    api(libs.spring.cloud.loadbalancer)
    // @HttpServiceFallback 的装配条件是 @ConditionalOnBean(CircuitBreakerFactory)。
    // 缺这个 starter 时降级不会报错，只是静默失效——所以按 api 依赖显式带上。
    api(libs.spring.cloud.starter.circuitbreaker.resilience4j)

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-discovery:kudos-ability-distributed-discovery-nacos"))
    testImplementation(libs.spring.boot.starter.web)
}
