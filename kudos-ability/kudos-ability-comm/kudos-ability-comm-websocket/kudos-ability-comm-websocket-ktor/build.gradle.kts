plugins {
    alias(libs.plugins.ktor) // 自带 Ktor BOM；与 kudos-ability-web-ktor 一致，无需在测试 dep 上声明版本号
}

dependencies {
    api(project(":kudos-context"))
    api(libs.ktor.server.websockets)

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(libs.ktor.server.test.host)
}
