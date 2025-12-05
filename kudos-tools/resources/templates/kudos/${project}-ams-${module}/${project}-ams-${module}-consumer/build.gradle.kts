dependencies {
    <#if project == "kudos">api(project(":kudos-ams:kudos-ams-${module}:kudos-ams-${module}-common"))
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))
    <#else>api(project(":${project}-ams-${module}:${project}-ams-${module}-common"))
    api("io.kudos:kudos-ability-distributed-client-feign")
    </#if>

    //region your codes 1

    //endregion your codes 1

    testImplementation(project(":kudos-test:kudos-test-container"))
}