dependencies {
    api(project(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-rdb:kudos-ability-log-audit-rdb-common"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(project(":kudos-test:kudos-test-rdb"))
    testImplementation(libs.h2database.h2)
    // Test H2 runs in MODE=PostgreSQL (see test-resources/application.yml); pull in Ktorm's PG
    // dialect via ServiceLoader so pagingSearch's LIMIT/OFFSET resolves to a real SQL clause
    // instead of failing with DialectFeatureNotSupportedException.
    testImplementation(libs.ktorm.support.postgresql)
}
