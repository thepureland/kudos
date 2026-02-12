dependencies {
    api(project(":kudos-context"))
    api(project(":kudos-base"))
    api(libs.lettuce.core)
    api(libs.spring.boot.starter.data.redis) {
        exclude(module = "io.lettuce")
    }
    api(libs.alibaba.fastjson2)
    api(libs.alibaba.fastjson2.spring)
    api(libs.apache.commons.pool2)

    testImplementation(project(":kudos-test:kudos-test-container"))
}