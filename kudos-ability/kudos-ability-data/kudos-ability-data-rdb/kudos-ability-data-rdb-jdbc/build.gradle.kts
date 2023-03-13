dependencies {
    api(project(":kudos-context"))
    api("org.soul:soul-ability-data-rdb-jdbc")

    // h2
    // h2可以用PostgreSqlDialect来实现分页
    compileOnly("com.h2database:h2:1.4.200")

    // postgres
    compileOnly("org.postgresql:postgresql:42.2.20")
    compileOnly("org.ktorm:ktorm-support-postgresql")

    testImplementation(project(":kudos-test:kudos-test-common"))
}