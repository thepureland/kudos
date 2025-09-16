dependencies {
    api(project(":kudos-context"))
    api("com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery")
    api("org.springframework.cloud:spring-cloud-loadbalancer")
    compileOnly("org.springframework.boot:spring-boot-starter-web")

    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation("org.springframework.boot:spring-boot-starter-web")
}