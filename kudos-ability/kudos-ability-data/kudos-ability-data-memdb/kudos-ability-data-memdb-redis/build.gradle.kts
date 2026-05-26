dependencies {
    api(project(":kudos-context"))
    api(project(":kudos-base"))
    // Use the lettuce version managed by the Spring Boot BOM to avoid NoSuchMethodError caused by incompatibility with the Spring Data Redis reactive API.
    api(libs.spring.boot.starter.data.redis)
    api(libs.alibaba.fastjson2)
    api(libs.alibaba.fastjson2.spring)
    api(libs.apache.commons.pool2)

    testImplementation(project(":kudos-test:kudos-test-container"))
}