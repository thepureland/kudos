dependencies {
    api(project(":kudos-ms:kudos-ms-user:kudos-ms-user-common"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-common"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-flyway"))

    //region your codes 1
    api(project(":kudos-ms:kudos-ms-sys:kudos-ms-sys-core"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-local:kudos-ability-cache-local-caffeine"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-remote:kudos-ability-cache-remote-redis"))

    // h2
    // h2可以用PostgreSqlDialect来实现分页
    testImplementation(libs.h2database.h2)

    // postgres
    testImplementation(libs.postgresql)
    testImplementation(libs.ktorm.support.postgresql)

    //endregion your codes 1

    testImplementation(project(":kudos-test:kudos-test-rdb"))
}
