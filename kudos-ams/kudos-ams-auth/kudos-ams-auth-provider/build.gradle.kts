dependencies {
    api(project(":kudos-ams:kudos-ams-auth:kudos-ams-auth-common"))
    api(project(":kudos-ams:kudos-ams-sys:kudos-ams-sys-provider"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-common"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-flyway"))
    
    //region your codes 1
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
