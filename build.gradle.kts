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
    // Move each submodule's build directory under the root project for unified management
    layout.buildDirectory = File(rootProject.projectDir, "build/${project.name}")

    // BOM/platform modules must be excluded: cannot apply kotlin-jvm / java / implementation configurations
    if (path == ":kudos-dependencies") {
        return@subprojects
    }

    // Apply the Kotlin JVM plugin to all submodules
    apply(plugin = "org.jetbrains.kotlin.jvm")
    // kotlin-spring: automatically adds `open` to Kotlin classes and methods annotated with @Component / @Service /
    // @Repository / @Configuration / @Controller / @Transactional / @Async / @Cacheable / @SpringBootTest, otherwise
    // Spring's CGLIB subclass proxies will emit "cannot get proxied via CGLIB" on every final method and silently
    // disable @Transactional / AOP aspects on those methods.
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")

    // Kotlin source directories
    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        sourceSets {
            getByName("main").kotlin.srcDirs("src")
            getByName("test").kotlin.srcDirs("test-src")
        }
    }

    // Resource directories
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
            // Optional: print standard out/err as events more explicitly
            // events("passed", "failed", "skipped", "standardOut", "standardError")
        }

        // Mockito's self-attached ByteBuddy agent is already a warning on JDK 21+ and will be disabled by default in
        // future versions. If testRuntimeClasspath contains mockito-core, attach it explicitly via -javaagent to avoid
        // future failures.
        //
        // Compatible with Gradle's configuration cache: resolve files during the "configuration phase", since the
        // closure can only capture finalized Providers/values and cannot access `project.configurations`.
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

    // Usage: ./gradlew publishToMavenLocal or ./gradlew :moduleName:publishToMavenLocal
    plugins.withId("java") {
        pluginManager.apply("maven-publish")

        extensions.configure<PublishingExtension>("publishing") {
            publications {
                // Avoid duplicate creation
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

// Determine whether a version is non-stable
fun String.isNonStable(): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    return !stableKeyword && !matches(regex)
}

// Run command: ./gradlew dependencyUpdates
tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        candidate.version.isNonStable() && !currentVersion.isNonStable()
    }
    checkForGradleUpdate = true
    outputFormatter = "json,plain"
    outputDir = "build/dependencyUpdates"
    reportfileName = "report"
}
