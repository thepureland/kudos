dependencies {
    api(project(":kudos-context"))
    api("org.springframework.boot:spring-boot-starter-data-redis")
    api("com.alibaba.fastjson2:fastjson2:2.0.58")
    api("com.alibaba.fastjson2:fastjson2-extension-spring5:2.0.58")
    api("org.apache.commons:commons-pool2:2.11.0")

    testImplementation(project(":kudos-test:kudos-test-container"))
}