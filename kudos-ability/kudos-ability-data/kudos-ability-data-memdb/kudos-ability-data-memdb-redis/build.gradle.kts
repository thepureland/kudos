dependencies {
    api(project(":kudos-context"))
    api(project(":kudos-base"))
    api(libs.spring.boot.starter.data.redis)
    api(libs.alibaba.fastjson2)
    api(libs.alibaba.fastjson2.spring)
    api(libs.apache.commons.pool2)

    testImplementation(project(":kudos-test:kudos-test-container"))
}