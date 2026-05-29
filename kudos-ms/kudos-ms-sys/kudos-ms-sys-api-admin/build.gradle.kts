dependencies {
    api(project(":kudos-ms:kudos-ms-sys:kudos-ms-sys-core"))
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-springmvc"))
    // Audit-log read-only API surface lives on the sys admin app so the console can query
    // sys_audit_log without dragging audit storage choice into the rest of the system. The
    // -rdb-ktorm impl is brought in here (not the bare -common) because the autoconfig there
    // registers the IAuditLogReadOnlyService bean we depend on.
    api(project(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-rdb:kudos-ability-log-audit-rdb-ktorm"))



    // h2
    // h2 can use PostgreSqlDialect to implement pagination
    testImplementation(libs.h2database.h2)

    // postgres
    testImplementation(libs.postgresql)
    testImplementation(libs.ktorm.support.postgresql)


    testImplementation(project(":kudos-test:kudos-test-rdb"))
}