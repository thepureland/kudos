val KTOR_VERSION: String = libs.versions.ktor.get()
val SPRING_BOOT_VERSION: String = libs.versions.springBoot.get()
val SPRING_CLOUD_VERSION: String = libs.versions.springCloud.get()
val ALIBABA_CLOUD__VERSION: String = libs.versions.alibabaCloud.get()

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
        add("implementation", platform("org.springframework.boot:spring-boot-dependencies:${SPRING_BOOT_VERSION}"))
        add("implementation", platform("org.springframework.cloud:spring-cloud-dependencies:${SPRING_CLOUD_VERSION}"))
        add("implementation", platform("com.alibaba.cloud:spring-cloud-alibaba-dependencies:${ALIBABA_CLOUD__VERSION}"))
        add("implementation", platform("io.ktor:ktor-bom:${KTOR_VERSION}"))
        add("testImplementation", kotlin("test-junit5")) // kotlin.test + JUnit5
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

}
















