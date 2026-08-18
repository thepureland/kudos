dependencies {
<#if project == "kudos">
    api(project(":kudos-ms:kudos-ms-${module}:kudos-ms-${module}-core"))
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-springmvc"))
<#else>
    api(project(":${project}-ms-${module}:${project}-ms-${module}-core"))
    api("io.kudos:kudos-ability-web-springmvc")
</#if>

    //region your codes 1

    // h2
    // H2 can use PostgreSqlDialect for pagination
    testImplementation(libs.h2database.h2)

    // postgres
    testImplementation(libs.postgresql)
    testImplementation(libs.ktorm.support.postgresql)

    //endregion your codes 1

<#if project == "kudos">
    testImplementation(project(":kudos-test:kudos-test-rdb"))
<#else>
    testImplementation("io.kudos:kudos-test-rdb")
</#if>
}
