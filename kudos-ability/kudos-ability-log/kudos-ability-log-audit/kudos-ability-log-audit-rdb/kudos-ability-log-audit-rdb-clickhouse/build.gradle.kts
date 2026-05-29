dependencies {
    api(project(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-rdb:kudos-ability-log-audit-rdb-common"))
    // Provides kudos's ktorm Database + Spring Tx bridge. We intentionally do NOT depend on
    // `kudos-ability-log-audit-rdb-ktorm` — that module ships the RdbKtormAuditService bean for
    // PostgreSQL/MySQL/H2, and pulling it in would land two IAuditService implementations side
    // by side. Apps that genuinely want both (rare) can declare both module deps + pick the
    // active one via @Primary on the business side.
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))

    // ClickHouse JDBC driver. Hosted on Maven Central, version pinned in libs.toml because
    // Spring Boot BOM doesn't manage it.
    api(libs.clickhouse.jdbc)

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(project(":kudos-test:kudos-test-container"))
}
