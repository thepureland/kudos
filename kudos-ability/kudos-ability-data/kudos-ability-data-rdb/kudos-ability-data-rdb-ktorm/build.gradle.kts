dependencies {
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc"))
    api("org.ktorm:ktorm-core")
    api("org.ktorm:ktorm-jackson")

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