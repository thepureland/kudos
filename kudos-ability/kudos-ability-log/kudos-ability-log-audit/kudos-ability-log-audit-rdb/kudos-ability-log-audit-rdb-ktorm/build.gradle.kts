dependencies {
    api(project(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-rdb:kudos-ability-log-audit-rdb-common"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(project(":kudos-test:kudos-test-rdb"))
    testImplementation(libs.h2database.h2)
}
