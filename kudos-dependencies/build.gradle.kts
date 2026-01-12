plugins {
    `java-platform`
    `maven-publish`
}


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

        // 3) 如果也想把“自己发布的各模块版本”放进 BOM（可选）
        // 前提：这些模块本身也会发布到仓库（有坐标）
        api("io.kudos:kudos-base:${project.version}")
        api("io.kudos:kudos-context:${project.version}")
        api("io.kudos:kudos-test-common:${project.version}")
        api("io.kudos:kudos-test-container:${project.version}")
    }
}

tasks.matching { it.name == "publishToMavenLocal" }.configureEach {
    dependsOn(
        ":kudos-base:publishToMavenLocal",
        ":kudos-context:publishToMavenLocal",
        ":kudos-test:kudos-test-common:publishToMavenLocal",
        ":kudos-test:kudos-test-container:publishToMavenLocal",
    )
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