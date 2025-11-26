dependencies {
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc"))
    api(libs.spring.boot.starter.flyway)

    // h2
    // h2可以用PostgreSqlDialect来实现分页
    testImplementation(libs.h2database.h2)

    // postgres
    testImplementation(libs.postgresql)
    testImplementation(libs.ktorm.support.postgresql)

    testImplementation(project(":kudos-test:kudos-test-container"))
}