dependencies {
<#if project == "kudos">
    api(project(":kudos-ms:kudos-ms-${module}:kudos-ms-${module}-core"))
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-springmvc"))
<#else>
    api(project(":${project}-ms-${module}:${project}-ms-${module}-core"))
    api("io.kudos:kudos-ability-web-springmvc")
</#if>

    //region your codes 1

    //endregion your codes 1

<#if project == "kudos">
    testImplementation(project(":kudos-test:kudos-test-container"))
<#else>
    testImplementation("io.kudos:kudos-test-container")
</#if>
}
