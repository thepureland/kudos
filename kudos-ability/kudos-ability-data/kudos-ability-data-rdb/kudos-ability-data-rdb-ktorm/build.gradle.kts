dependencies {
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc"))
    api("org.ktorm:ktorm-core")
    api("org.ktorm:ktorm-jackson")

    testImplementation(project(":kudos-test:kudos-test-common"))
}