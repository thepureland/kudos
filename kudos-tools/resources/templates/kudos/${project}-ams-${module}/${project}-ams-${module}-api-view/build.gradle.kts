dependencies {
    <#if project == "kudos">
    api(project(":kudos-ams:kudos-ams-${module}:kudos-ams-${module}-service"))
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-ktor"))
    <#else>
    api(project(":${project}-ams-${module}:${project}-ams-${module}-service"))
    api("io.kudos:kudos-ability-web-ktor")
    </#if>

    //region your codes 1

    //endregion your codes 1

    testImplementation(project(":kudos-test:kudos-test-container"))
}