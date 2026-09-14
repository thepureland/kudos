dependencies {
    api(project(":kudos-ms:kudos-ms-tag:kudos-ms-tag-sql"))
    api(project(":kudos-ms:kudos-ms-tag:kudos-ms-tag-common"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-common"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-flyway"))
    api(libs.flyway.mysql)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.h2database.h2)
    testImplementation(libs.postgresql)
    testImplementation("com.mysql:mysql-connector-j")
    testImplementation(libs.ktorm.support.postgresql)
    testImplementation(libs.ktorm.support.mysql)
    testImplementation(project(":kudos-test:kudos-test-rdb"))
    testImplementation(project(":kudos-test:kudos-test-container"))
}
