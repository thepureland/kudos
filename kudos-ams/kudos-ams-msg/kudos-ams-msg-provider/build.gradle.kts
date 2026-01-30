dependencies {
    api(project(":kudos-ams:kudos-ams-msg:kudos-ams-msg-common"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-common"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-flyway"))

    //region your codes 1
    api(project(":kudos-ams:kudos-ams-user:kudos-ams-user-provider"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-local:kudos-ability-cache-local-caffeine"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-remote:kudos-ability-cache-remote-redis"))

    // H2
    // H2 can use PostgreSqlDialect for paging
    testImplementation(libs.h2database.h2)

    // postgres
    testImplementation(libs.postgresql)
    testImplementation(libs.ktorm.support.postgresql)

    //endregion your codes 1

    testImplementation(project(":kudos-test:kudos-test-rdb"))
}
