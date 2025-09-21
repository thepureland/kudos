dependencies {
    <#if project == "kudos">api(project(":kudos-ams:kudos-ams-${module}:kudos-ams-${module}-common"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-common"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-flyway"))
    <#else>api(project(":${project}-ams-${module}:${project}-ams-${module}-common"))
    api("io.kudos:kudos-ability-cache-common")
    api("io.kudos:kudos-ability-data-rdb-ktorm")
    api("io.kudos:kudos-ability-data-rdb-flyway")
    </#if>

    //region your codes 1
    <#if project == "kudos">api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-local:kudos-ability-cache-local-caffeine"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-remote:kudos-ability-cache-remote-redis"))
    <#else>api("io.kudos:kudos-ability-cache-local-caffeine")
    api("io.kudos:kudos-ability-cache-remote-redis")
    </#if>

    // h2
    // h2可以用PostgreSqlDialect来实现分页
    testImplementation("com.h2database:h2:${libs.versions.h2.get()}")

    // postgres
    testImplementation("org.postgresql:postgresql:${libs.versions.postgres.get()}")
    testImplementation("org.ktorm:ktorm-support-postgresql:${libs.versions.ktorm.get()}")

    //endregion your codes 1

    testImplementation(project(":kudos-test:kudos-test-container"))
}