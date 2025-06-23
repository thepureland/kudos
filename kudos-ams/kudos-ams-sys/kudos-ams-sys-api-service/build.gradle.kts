dependencies {
    api(project(":kudos-ams:kudos-ams-sys:kudos-ams-sys-service"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-provider"))
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-discovery:kudos-ability-distributed-discovery-nacos"))
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-config:kudos-ability-distributed-config-nacos"))
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-ktor"))

    // h2
    // h2可以用PostgreSqlDialect来实现分页
    testImplementation("com.h2database:h2:2.3.232")

    // postgres
    testImplementation("org.postgresql:postgresql:42.7.5")
    testImplementation("org.ktorm:ktorm-support-postgresql:4.1.1")

    testImplementation("org.flywaydb:flyway-core:11.9.1")

    testImplementation(project(":kudos-test:kudos-test-container"))
}