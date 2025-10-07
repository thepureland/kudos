plugins {
    alias(libs.plugins.ktor) // 自动带入 BOM
}

dependencies {
    api(project(":kudos-context"))

    // 配置文件支持yaml
    api(libs.ktor.server.core)
//    api("io.ktor:ktor-server-test-host-jvm")
    api(libs.ktor.server.config.yaml)
    api(libs.ktor.server.content.negotiation)
    api(libs.ktor.server.status.pages)
    api(libs.ktor.server.websockets)

    // 日志
//    api("io.ktor:ktor-server-call-logging-jvm")
//    // CORS 插件示例
//    api("io.ktor:ktor-server-cors-jvm")
//    // 服务端推送SSE
//    api("io.ktor:ktor-server-sse-jvm")
//    // CSRF保护
//    api("io.ktor:ktor-server-csrf-jvm")

//    api("ch.qos.logback:logback-classic:1.5.18")
//    api("commons-logging:commons-logging:1.3.5")


    // 测试相关
//    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:${libs.versions.kotlin.get()}")
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.server.netty)
    testImplementation(libs.ktor.server.jetty.jakarta)
    testImplementation(libs.ktor.server.tomcat.jakarta)
    testImplementation(libs.ktor.server.cio)
    testImplementation(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))

    testImplementation(project(":kudos-test:kudos-test-common"))
}

application {
    // 指定 application 插件的主类，这样打包后，Jar 的 Manifest 会把 Main-Class: io.ktor.server.netty.EngineMain 写进去
    mainClass.set("io.ktor.server.netty.EngineMain")
}
