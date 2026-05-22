plugins {
    `java-platform`
    `maven-publish`
}

val publishedKudosProjects = rootProject.subprojects
    .filterNot { it.path == project.path }
    .sortedBy { it.path }

javaPlatform {
    // 允许 BOM 自己引用别的 BOM（比如 Spring 的）
    allowDependencies()
}

dependencies {
    // 1) 可以先引入别人的 BOM
//    api(platform("org.springframework.boot:spring-boot-dependencies:3.3.6"))

    // 2) 自己的统一版本（相当于 dependencyManagement）
    constraints {
//        api("com.google.guava:guava:33.2.1-jre")

        // 3) kudos 自身各子模块版本。保持与 settings.gradle.kts 中 include 的项目同步，
        // 避免新增模块后忘记手动维护 BOM 约束。
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
        // 本地测试
        mavenLocal()

        // 私服
        // maven {
        //     url = uri("https://your-nexus/repository/maven-releases/")
        //     credentials { username = findProperty("repoUser") as String; password = findProperty("repoPass") as String }
        // }
    }
}
