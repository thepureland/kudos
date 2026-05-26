dependencies {
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc"))
    api(libs.ktorm.core)
    api(libs.ktorm.jackson)

    // h2
    // h2 can use PostgreSqlDialect to implement pagination
    testImplementation(libs.h2database.h2)

    // postgres
    testImplementation(libs.postgresql)
    testImplementation(libs.ktorm.support.postgresql)

    testImplementation(project(":kudos-test:kudos-test-common"))
}