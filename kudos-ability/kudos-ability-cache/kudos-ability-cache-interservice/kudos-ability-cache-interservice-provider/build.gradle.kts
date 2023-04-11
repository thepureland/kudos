dependencies {
    api("org.soul:soul-ability-cache-interservice-provider")
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-common"))
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-springmvc"))

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client"))
    testImplementation(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-client"))
    testImplementation(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-local:kudos-ability-cache-local-caffeine"))
}

sourceSets {
    main {
        kotlin {
            setSrcDirs(listOf("src"))
        }
        java {
            setSrcDirs(listOf("src"))
        }
        resources {
            setSrcDirs(listOf("resources"))
        }
    }
    test {
        kotlin {
            setSrcDirs(listOf("test-src"))
        }
        java {
            setSrcDirs(listOf("test-src"))
        }
        resources {
            setSrcDirs(listOf("test-resources"))
        }
    }
}