dependencies {
    <#if project == "kudos">api(project(":kudos-ms:kudos-ms-${module}:kudos-ms-${module}-common"))
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
    testImplementation(libs.h2database.h2)

    // postgres
    testImplementation(libs.postgresql)
    testImplementation(libs.ktorm.support.postgresql)

    //endregion your codes 1

    testImplementation(project(":kudos-test:kudos-test-container"))
}