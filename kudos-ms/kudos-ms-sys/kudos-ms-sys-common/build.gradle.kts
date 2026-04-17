import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

dependencies {
    api(project(":kudos-context"))
    compileOnly(platform(libs.spring.boot.bom))
    compileOnly("com.fasterxml.jackson.core:jackson-annotations")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        freeCompilerArgs.add("-Xjvm-default=all")
        javaParameters.set(true)  // 保留构造参数名，供 Jackson 等反射使用
    }
}