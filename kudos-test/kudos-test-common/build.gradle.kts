dependencies {
    api(project(":kudos-base"))
    api("org.springframework.boot:spring-boot-starter-aop")
    api("org.springframework.boot:spring-boot-starter-test")

    // h2
    // h2可以用PostgreSqlDialect来实现分页
    api("com.h2database:h2")

    // postgres
    api("org.postgresql:postgresql")
    api("org.ktorm:ktorm-support-postgresql")
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