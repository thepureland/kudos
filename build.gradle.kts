
val KTOR_VERSION: String = libs.versions.ktor.get()


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
        add("implementation", platform("org.springframework:spring-framework-bom:6.1.16"))
        add("implementation", platform("org.springframework.boot:spring-boot-dependencies:3.4.3"))
        add("implementation", platform("org.springframework.cloud:spring-cloud-dependencies:2024.0.0"))
        add("implementation", platform("com.alibaba.cloud:spring-cloud-alibaba-dependencies:2023.0.1.2"))
        add("implementation", platform("io.ktor:ktor-bom:${KTOR_VERSION}"))
        add("testImplementation", kotlin("test-junit5")) // kotlin.test + JUnit5
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }


}
















