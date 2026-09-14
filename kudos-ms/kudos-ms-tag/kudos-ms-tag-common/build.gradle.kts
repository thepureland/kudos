import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    api(project(":kudos-context"))
    api(libs.kotlinxSerialization)
    compileOnly(platform(libs.spring.boot.bom))
    compileOnly("com.fasterxml.jackson.core:jackson-annotations")
    compileOnly("org.springframework:spring-web")

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.boot.starter.test)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        freeCompilerArgs.add("-Xjvm-default=all")
        javaParameters.set(true)
    }
}
