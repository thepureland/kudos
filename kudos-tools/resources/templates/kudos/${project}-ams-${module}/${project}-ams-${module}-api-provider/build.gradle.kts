dependencies {
    <#if project == "kudos">api(project(":kudos-ams:kudos-ams-${module}:kudos-ams-${module}-provider"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-provider"))
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-discovery:kudos-ability-distributed-discovery-nacos"))
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-config:kudos-ability-distributed-config-nacos"))
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-springmvc"))
    <#else>api(project(":${project}-ams-${module}:${project}-ams-${module}-provider"))
    api("io.kudos:kudos-ability-cache-interservice-provider")
    api("io.kudos:kudos-ability-distributed-discovery-nacos")
    api("io.kudos:kudos-ability-distributed-config-nacos")
    api("io.kudos:kudos-ability-web-springmvc")
    </#if>

    //region your codes 1


    //endregion your codes 1

    testImplementation(project(":kudos-test:kudos-test-container"))
}