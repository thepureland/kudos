dependencies {
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-lock:kudos-ability-distributed-lock-common"))
    api("org.redisson:redisson:3.50.0")

    testImplementation(project(":kudos-test:kudos-test-container"))
}