dependencies {
    api(project(":kudos-context"))
    api("org.soul:soul-ability-data-memdb-redis")

    testImplementation(project(":kudos-test:kudos-test-container"))
}