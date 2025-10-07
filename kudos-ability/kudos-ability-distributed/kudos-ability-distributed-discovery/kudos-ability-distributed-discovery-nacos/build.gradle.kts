dependencies {
    api(project(":kudos-context"))
    api(libs.alibaba.cloud.nacos.discovery)
    api(libs.spring.cloud.loadbalancer)
    compileOnly(libs.spring.boot.starter.web)

    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation(libs.spring.boot.starter.web)
}