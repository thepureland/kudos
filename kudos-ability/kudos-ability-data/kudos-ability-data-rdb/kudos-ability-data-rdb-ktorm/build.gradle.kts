dependencies {
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc"))
    api("org.ktorm:ktorm-core:4.1.1")
    api("org.ktorm:ktorm-jackson:4.1.1")

    // h2
    // h2可以用PostgreSqlDialect来实现分页
    testImplementation("com.h2database:h2:2.3.232")

    // postgres
    testImplementation("org.postgresql:postgresql:42.7.5")
    testImplementation("org.ktorm:ktorm-support-postgresql:4.1.1")

//    // mysql
//    testImplementation("mysql:mysql-connector-java:8.0.33")
//    testImplementation("org.ktorm:ktorm-support-mysql:4.1.1")

    // sqlite
//    testImplementation("org.ktorm:ktorm-support-sqlite")
//    testImplementation("org.xerial:sqlite-jdbc:3.30.1")

    // oracle
//    testImplementation("com.oracle.database.jdbc:ojdbc10:19.11.0.0")
//    testImplementation("org.ktorm:ktorm-support-oracle")

    // sqlserver
//    testImplementation("com.microsoft.sqlserver:mssql-jdbc:9.2.1.jre11")
//    testImplementation("org.ktorm:ktorm-support-sqlserver")

    testImplementation(project(":kudos-test:kudos-test-container"))
}