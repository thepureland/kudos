dependencies {
    api(project(":kudos-context"))
    api(project(":kudos-base"))
    // 使用 Spring Boot BOM 管理的 lettuce 版本，避免与 Spring Data Redis 的 reactive API 不兼容导致 NoSuchMethodError
    api(libs.spring.boot.starter.data.redis)
    api(libs.alibaba.fastjson2)
    api(libs.alibaba.fastjson2.spring)
    api(libs.apache.commons.pool2)

    testImplementation(project(":kudos-test:kudos-test-container"))
}