dependencies {
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-common"))
    api(libs.spring.boot.starter.web)
    // 只用到 RestClientHttpServiceGroupConfigurer / ClientHttpRequestInterceptor，装配上有
    // @ConditionalOnClass 兜底，所以按 compileOnly 依赖，不把 interface client 强加给调用方。
    compileOnly(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-http"))

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-http"))
}
