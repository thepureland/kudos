dependencies {
    api(project(":kudos-context"))
    api("org.soul:soul-ability-data-rdb-jdbc")

    testImplementation("com.h2database:h2:2.3.232")
    testImplementation(project(":kudos-test:kudos-test-common"))
}