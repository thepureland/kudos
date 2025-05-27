plugins {
    kotlin("plugin.serialization") version "2.1.21"
    id("io.ktor.plugin") version "3.1.3" // 自动带入 BOM
    application
}

dependencies {
    api(project(":kudos-context"))

    // 核心功能（包含 routing 插件）
    api("io.ktor:ktor-server-core-jvm")
    // Netty 引擎
    api("io.ktor:ktor-server-netty-jvm")
    // 可选模块，内容协商 + JSON
    api("io.ktor:ktor-server-content-negotiation-jvm")
    api("io.ktor:ktor-serialization-kotlinx-json-jvm")
    // 日志
    api("io.ktor:ktor-server-call-logging-jvm")
    // 状态页
    api("io.ktor:ktor-server-status-pages-jvm")
    // CORS 插件示例
    api("io.ktor:ktor-server-cors-jvm")
    // 服务端推送SSE
    api("io.ktor:ktor-server-sse-jvm")
    // CSRF保护
    api("io.ktor:ktor-server-csrf-jvm")

    // 测试相关
    testImplementation("io.ktor:ktor-server-test-host-jvm")
//    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}

application {
    // 1. 指定 application 插件的主类
    mainClass.set("io.ktor.server.netty.EngineMain")
}
