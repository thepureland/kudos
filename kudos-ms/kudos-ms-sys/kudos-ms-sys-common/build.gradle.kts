import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

dependencies {
    api(project(":kudos-context"))
    compileOnly(platform(libs.spring.boot.bom))
    compileOnly("com.fasterxml.jackson.core:jackson-annotations")
    // 供 ISysXxxApi 使用方法级 @GetExchange / @PostExchange / @RequestParam / @RequestBody：
    // 同一份声明既是 interface client 的客户端契约，也是 Spring MVC 7 的服务端 request mapping。
    // 只能加在方法上；**不要**在接口类型上加 @RequestMapping 或 @HttpExchange——前者会让任何实现它的
    // @Component 被当成 Spring MVC handler，后者在 Spring MVC 7 里同样被 isHandler() 识别，
    // 还会让客户端代理 bean 被注册成 controller（Ambiguous mapping）。
    compileOnly("org.springframework:spring-web")

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.boot.starter.test)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        freeCompilerArgs.add("-Xjvm-default=all")
        javaParameters.set(true)  // 保留参数名供 Jackson 与 interface client 的 @RequestParam 反射取名（根 build 已全局开启，此处保留为显式声明）
    }
}