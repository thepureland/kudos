dependencies {
    api(project(":kudos-ams:kudos-ams-sys:kudos-ams-sys-common"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-common"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-flyway"))
    
    //region your codes 1
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-local:kudos-ability-cache-local-caffeine"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-remote:kudos-ability-cache-remote-redis"))

    // h2
    // h2可以用PostgreSqlDialect来实现分页
    testImplementation("com.h2database:h2:${libs.versions.h2.get()}")

    // postgres
    testImplementation("org.postgresql:postgresql:${libs.versions.postgres.get()}")
    testImplementation("org.ktorm:ktorm-support-postgresql:${libs.versions.ktorm.get()}")

    //endregion your codes 1

    testImplementation(project(":kudos-test:kudos-test-container"))
}