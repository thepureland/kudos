
object Version {
    const val SOUL = "5.4.1-SNAPSHOT"
}


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
        add("implementation", platform("org.soul:soul-dependencies:${Version.SOUL}"))
        add("testImplementation", kotlin("test-junit5")) // kotlin.test + JUnit5
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }


}
















