dependencies {
    api(project(":kudos-context"))
    api(libs.alibaba.cloud.nacos.config)

    testImplementation(project(":kudos-test:kudos-test-container"))
}