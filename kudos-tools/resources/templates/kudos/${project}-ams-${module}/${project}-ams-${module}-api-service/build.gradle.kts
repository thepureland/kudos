dependencies {
    <#if project == "kudos">api(project(":kudos-ams:kudos-ams-${module}:kudos-ams-${module}-service"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-provider"))
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-discovery:kudos-ability-distributed-discovery-nacos"))
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-config:kudos-ability-distributed-config-nacos"))
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-ktor"))
    <#else>api(project(":${project}-ams-${module}:${project}-ams-${module}-service"))
    api("io.kudos:kudos-ability-cache-interservice-provider")
    api("io.kudos:kudos-ability-distributed-discovery-nacos")
    api("io.kudos:kudos-ability-distributed-config-nacos")
    api("io.kudos:kudos-ability-web-ktor")
    </#if>

    //region your codes 1

    // h2
    // h2可以用PostgreSqlDialect来实现分页
    testImplementation("com.h2database:h2:${libs.versions.h2.get()}")

    // postgres
    testImplementation("org.postgresql:postgresql:${libs.versions.postgres.get()}")
    testImplementation("org.ktorm:ktorm-support-postgresql:${libs.versions.ktorm.get()}")

    //endregion your codes 1

    testImplementation(project(":kudos-test:kudos-test-container"))
}