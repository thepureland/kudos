dependencies {
    api(project(":kudos-ms:kudos-ms-sys:kudos-ms-sys-core"))
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-springmvc"))
    

    // h2
    // h2 can use PostgreSqlDialect to implement pagination
    testImplementation(libs.h2database.h2)

    // postgres
    testImplementation(libs.postgresql)
    testImplementation(libs.ktorm.support.postgresql)


    testImplementation(project(":kudos-test:kudos-test-rdb"))
}