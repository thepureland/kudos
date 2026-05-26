import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

dependencies {
    api(project(":kudos-context"))
    compileOnly(platform(libs.spring.boot.bom))
    compileOnly("com.fasterxml.jackson.core:jackson-annotations")
    // Used to put method-level @GetMapping/@PostMapping/@RequestParam/@RequestBody on ISysXxxApi so Feign proxies can recognize them.
    // Method-level only; do not put @RequestMapping on the interface type — otherwise any @Component implementing it would be treated as a Spring MVC handler.
    compileOnly("org.springframework:spring-web")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        freeCompilerArgs.add("-Xjvm-default=all")
        javaParameters.set(true)  // Preserve constructor parameter names for use by Jackson and other reflection
    }
}