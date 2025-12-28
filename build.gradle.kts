import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

val spring_boot_bom = libs.spring.boot.bom
val spring_cloud_bom = libs.spring.cloud.bom
val spring_ai_bom = libs.spring.ai.bom
val alibaba_cloud_bom = libs.alibaba.cloud.bom
val ktor_bom = libs.ktor.bom

subprojects {
    // 所有子模块都应用 Kotlin JVM 插件
    apply(plugin = "org.jetbrains.kotlin.jvm")

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

    // 每个子模块的 build 目录挪到根项目下统一管理
    layout.buildDirectory = File(rootProject.projectDir, "build/${project.name}")

    dependencies {
        add("implementation", platform(spring_boot_bom))
        add("implementation", platform(spring_cloud_bom))
        add("implementation", platform(spring_ai_bom))
        add("implementation", platform(alibaba_cloud_bom))
        add("implementation", platform(ktor_bom))
//      add("testImplementation", libs.kotlin.test.junit5)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
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
