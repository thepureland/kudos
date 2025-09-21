dependencies {
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc"))
    api("org.ktorm:ktorm-core:${libs.versions.ktorm.get()}")
    api("org.ktorm:ktorm-jackson:${libs.versions.ktorm.get()}")

    // h2
    // h2可以用PostgreSqlDialect来实现分页
    testImplementation("com.h2database:h2:${libs.versions.h2.get()}")

    // postgres
    testImplementation("org.postgresql:postgresql:${libs.versions.postgres.get()}")
    testImplementation("org.ktorm:ktorm-support-postgresql:${libs.versions.ktorm.get()}")

    testImplementation(project(":kudos-test:kudos-test-common"))
}