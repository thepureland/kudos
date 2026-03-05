dependencies {
    api(project(":kudos-ms:kudos-ms-sys:kudos-ms-sys-core"))
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-springmvc"))
    
    //region your codes 1

    // h2
    // h2可以用PostgreSqlDialect来实现分页
    testImplementation(libs.h2database.h2)

    // postgres
    testImplementation(libs.postgresql)
    testImplementation(libs.ktorm.support.postgresql)

    //endregion your codes 1

    testImplementation(project(":kudos-test:kudos-test-container"))
}