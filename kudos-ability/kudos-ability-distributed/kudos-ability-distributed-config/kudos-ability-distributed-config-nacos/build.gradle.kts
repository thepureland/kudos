dependencies {
    api(project(":kudos-context"))
    api("com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-config")

    testImplementation(project(":kudos-test:kudos-test-container"))
}