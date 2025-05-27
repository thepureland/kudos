dependencies {
    api(project(":kudos-context"))
    api("org.soul:soul-ability-distributed-stream-rabbit")

    testImplementation(project(":kudos-test:kudos-test-container"))
}