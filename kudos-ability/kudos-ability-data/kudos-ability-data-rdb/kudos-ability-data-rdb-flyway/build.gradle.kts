dependencies {
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc"))
    api(libs.spring.boot.starter.flyway)
    api(libs.baomidou.dynamic.datasource.starter)

    // h2
    // h2 can use PostgreSqlDialect to implement pagination
    testImplementation(libs.h2database.h2)

    // postgres
    testImplementation(libs.postgresql)
    testImplementation(libs.ktorm.support.postgresql)

    testImplementation(project(":kudos-test:kudos-test-container"))
}