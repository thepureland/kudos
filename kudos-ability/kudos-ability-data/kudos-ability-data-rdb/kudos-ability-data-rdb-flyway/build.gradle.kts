dependencies {
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc"))
    api("org.flywaydb:flyway-core:11.9.1")

    // h2
    // h2可以用PostgreSqlDialect来实现分页
    testImplementation("com.h2database:h2:2.3.232")

    // postgres
    testImplementation("org.postgresql:postgresql:42.7.5")
    testImplementation("org.ktorm:ktorm-support-postgresql:4.1.1")

    testImplementation(project(":kudos-test:kudos-test-container"))
}