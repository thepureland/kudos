dependencies {
    api(project(":kudos-ams:kudos-ams-sys:kudos-ams-sys-common"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-flyway"))

    // h2
    // h2可以用PostgreSqlDialect来实现分页
    testImplementation("com.h2database:h2:2.3.232")

    // postgres
    testImplementation("org.postgresql:postgresql:42.7.5")
    testImplementation("org.ktorm:ktorm-support-postgresql:4.1.1")

    testImplementation("org.flywaydb:flyway-core:11.9.1")

    testImplementation(project(":kudos-test:kudos-test-container"))
}