dependencies {
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc"))
    api("org.ktorm:ktorm-core")
    api("org.ktorm:ktorm-jackson")

    // h2
    // h2可以用PostgreSqlDialect来实现分页
    compileOnly("com.h2database:h2:1.4.200")

    // postgres
    compileOnly("org.postgresql:postgresql:42.2.20")
    compileOnly("org.ktorm:ktorm-support-postgresql")

    testImplementation(project(":kudos-test:kudos-test-common"))
}