dependencies {
    api(project(":kudos-context"))
    api("org.soul:soul-ability-distributed-lock-redisson")

    testImplementation(project(":kudos-test:kudos-test-container"))
}