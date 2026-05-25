// The code in this file is a convention plugin - a Gradle mechanism for sharing reusable build logic.
// `buildSrc` is a Gradle-recognized directory and every plugin there will be easily available in the rest of the build.
package buildsrc.convention

import org.gradle.api.tasks.testing.logging.TestLogEvent

// 注意：这是 buildSrc 里的"约定插件"，但仓库根 build.gradle.kts 当前未通过 `apply(plugin = ...)`
// 引入它，所以本文件实际处于未启用状态。kotlin-jvm 插件与 Test 任务相关的统一配置
// （JUnitPlatform、测试日志、kotlin-spring allopen、Mockito javaagent 等）目前都放在
// 根 build.gradle.kts 的 `subprojects { ... }` 块里直接应用。
// 保留本文件以便后续真正切回约定插件方案时复用。
plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin in JVM projects.
    kotlin("jvm")
}

kotlin {
    // Use a specific Java version to make it easier to work in different environments.
    jvmToolchain(25)
}

tasks.withType<Test>().configureEach {
    // Configure all test Gradle tasks to use JUnitPlatform.
    useJUnitPlatform()

    // Log information about all test results, not only the failed ones.
    testLogging {
        events(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED
        )
    }
}
