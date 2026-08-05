dependencies {
    api(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-sql"))
    api(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-common"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-common"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-flyway"))
    
    api(project(":kudos-ms:kudos-ms-user:kudos-ms-user-core"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-local:kudos-ability-cache-local-caffeine"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-remote:kudos-ability-cache-remote-redis"))

    // Implements the enforcement layer's ports (IAuthzDecisionProvider / IPermissionPointRegistry).
    // The dependency runs ms -> ability on purpose: the ability module must stay free of any
    // microservice dependency so a different authorization backend can supply the same ports.
    api(project(":kudos-ability:kudos-ability-security:kudos-ability-security-enforcement"))

    // h2
    // h2 supports pagination via PostgreSqlDialect
    testImplementation(libs.h2database.h2)

    // postgres
    testImplementation(libs.postgresql)
    testImplementation(libs.ktorm.support.postgresql)


    testImplementation(project(":kudos-test:kudos-test-rdb"))
}
