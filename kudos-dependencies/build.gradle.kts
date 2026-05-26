plugins {
    `java-platform`
    `maven-publish`
}

val publishedKudosProjects = rootProject.subprojects
    .filterNot { it.path == project.path }
    .sortedBy { it.path }

javaPlatform {
    // Allow this BOM to reference other BOMs (e.g. Spring's)
    allowDependencies()
}

dependencies {
    // 1) Optionally import third-party BOMs first
//    api(platform("org.springframework.boot:spring-boot-dependencies:3.3.6"))

    // 2) Our own unified versions (equivalent to dependencyManagement)
    constraints {
//        api("com.google.guava:guava:33.2.1-jre")

        // 3) Versions for kudos's own submodules. Keep in sync with the projects included in
        // settings.gradle.kts so BOM constraints are not forgotten when new modules are added.
        publishedKudosProjects.forEach { kudosProject ->
            api("${kudosProject.group}:${kudosProject.name}:${kudosProject.version}")
        }
    }
}

tasks.matching { it.name == "publishToMavenLocal" }.configureEach {
    dependsOn(publishedKudosProjects.map { "${it.path}:publishToMavenLocal" })
}

publishing {
    publications {
        create<MavenPublication>("mavenBom") {
            from(components["javaPlatform"])
            artifactId = "kudos-dependencies"
        }
    }
    repositories {
        // Local testing
        mavenLocal()

        // Private repository
        // maven {
        //     url = uri("https://your-nexus/repository/maven-releases/")
        //     credentials { username = findProperty("repoUser") as String; password = findProperty("repoPass") as String }
        // }
    }
}
