dependencies {
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-common"))
    api("org.soul:soul-ability-web-springmvc")

    testImplementation(project(":kudos-test:kudos-test-common"))
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