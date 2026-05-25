import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

val springBootBom = libs.spring.boot.bom
val springCloudBom = libs.spring.cloud.bom
val alibabaCloudBom = libs.alibaba.cloud.bom
val ktorBom = libs.ktor.bom

allprojects {
    group = "io.kudos"
    version = "1.0.0-SNAPSHOT"
}

subprojects {
    // 每个子模块的 build 目录挪到根项目下统一管理
    layout.buildDirectory = File(rootProject.projectDir, "build/${project.name}")

    // BOM/platform 模块要排除掉：不能套 kotlin-jvm / java / implementation 等配置
    if (path == ":kudos-dependencies") {
        return@subprojects
    }

    // 所有子模块都应用 Kotlin JVM 插件
    apply(plugin = "org.jetbrains.kotlin.jvm")
    // kotlin-spring: 自动给标注了 @Component / @Service / @Repository / @Configuration / @Controller
    // / @Transactional / @Async / @Cacheable / @SpringBootTest 的 Kotlin 类与方法加 `open`，否则
    // Spring CGLIB 子类代理会在每个 final 方法上输出 "cannot get proxied via CGLIB"，并使
    // @Transactional / AOP 切面对这些方法静默失效。
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")

    // Kotlin 源码目录
    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        sourceSets {
            getByName("main").kotlin.srcDirs("src")
            getByName("test").kotlin.srcDirs("test-src")
        }
    }

    // 资源目录
    the<JavaPluginExtension>().sourceSets {
        getByName("main").resources.srcDir("resources")
        getByName("test").resources.srcDir("test-resources")
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        dependencies {
            add("implementation", platform(springBootBom))
            add("implementation", platform(springCloudBom))
            add("implementation", platform(alibabaCloudBom))
            add("implementation", platform(ktorBom))
//      add("testImplementation", libs.kotlin.test.junit5)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
            // 可选：更明确地把标准输出/错误也当作事件打印
            // events("passed", "failed", "skipped", "standardOut", "standardError")
        }

        // Mockito 自挂载 ByteBuddy agent 的方式在 JDK 21+ 已是 warn，未来版本将默认禁止。
        // 若 testRuntimeClasspath 含 mockito-core，则显式以 -javaagent 形式挂载，规避将来失败。
        //
        // 兼容 Gradle 配置缓存：必须在「配置阶段」就把文件解析出来，闭包里只能闭合
        // 已经定型的 Provider/值，不能再访问 `project.configurations`。
        val mockitoAgentJar = project.configurations.findByName("testRuntimeClasspath")
            ?.incoming
            ?.artifactView { lenient(true) }
            ?.files
            ?.filter { it.name.startsWith("mockito-core-") && it.name.endsWith(".jar") }
        if (mockitoAgentJar != null) {
            jvmArgumentProviders.add(
                CommandLineArgumentProvider {
                    val jar = runCatching { mockitoAgentJar.singleOrNull() ?: mockitoAgentJar.firstOrNull() }.getOrNull()
                    if (jar != null) listOf("-javaagent:${jar.absolutePath}") else emptyList()
                }
            )
        }
    }

    // 用法：./gradlew publishToMavenLocal 或 ./gradlew :模块名:publishToMavenLocal
    plugins.withId("java") {
        pluginManager.apply("maven-publish")

        extensions.configure<PublishingExtension>("publishing") {
            publications {
                // 避免重复创建
                if (findByName("mavenJava") == null) {
                    create<MavenPublication>("mavenJava") {
                        from(components["java"])
                        artifactId = project.name
                    }
                }
            }
        }
    }

}

plugins {
    alias(libs.plugins.github.ben.manes)
}

// 判定是否为非稳定版
fun String.isNonStable(): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    return !stableKeyword && !matches(regex)
}

// 執行命令：./gradlew dependencyUpdates
tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        candidate.version.isNonStable() && !currentVersion.isNonStable()
    }
    checkForGradleUpdate = true
    outputFormatter = "json,plain"
    outputDir = "build/dependencyUpdates"
    reportfileName = "report"
}
