import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

val spring_boot_bom = libs.spring.boot.bom
val spring_cloud_bom = libs.spring.cloud.bom
val alibaba_cloud_bom = libs.alibaba.cloud.bom
val ktor_bom = libs.ktor.bom

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        sourceSets {
            getByName("main").kotlin.srcDirs("src")
            getByName("test").kotlin.srcDirs("test-src")
        }
    }

    the<JavaPluginExtension>().sourceSets {
        getByName("main").resources.srcDir("resources")
        getByName("test").resources.srcDir("test-resources")
    }

    layout.buildDirectory = File(rootProject.projectDir, "build/${project.name}")

    dependencies {
        add("implementation", platform(spring_boot_bom))
        add("implementation", platform(spring_cloud_bom))
        add("implementation", platform(alibaba_cloud_bom))
        add("implementation", platform(ktor_bom))
        add("testImplementation", kotlin("test-junit5")) // kotlin.test + JUnit5
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
    val regex = "^[0-9,.v-]+(-r)?$".toRegex() // 纯数字或常见稳定尾缀
    return !stableKeyword && !matches(regex)
}

tasks.withType<DependencyUpdatesTask> {
    // 只提示稳定版
    rejectVersionIf {
        candidate.version.isNonStable() && !currentVersion.isNonStable()
    }
    // 同时检查 Gradle 自身是否可更新
    checkForGradleUpdate = true
    // 输出：plain/json/xml/html
    outputFormatter = "json,plain"
    outputDir = "build/dependencyUpdates"
    reportfileName = "report"
}
















