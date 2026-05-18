import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

dependencies {
    api(project(":kudos-context"))
    compileOnly(platform(libs.spring.boot.bom))
    compileOnly("com.fasterxml.jackson.core:jackson-annotations")
    // 用于在 ISysXxxApi 上挂方法级 @GetMapping/@PostMapping/@RequestParam/@RequestBody，让 Feign 代理可识别。
    // 仅方法级，不在接口类型上放 @RequestMapping —— 否则实现该接口的 @Component 会被 Spring MVC 当成 handler。
    compileOnly("org.springframework:spring-web")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        freeCompilerArgs.add("-Xjvm-default=all")
        javaParameters.set(true)  // 保留构造参数名，供 Jackson 等反射使用
    }
}