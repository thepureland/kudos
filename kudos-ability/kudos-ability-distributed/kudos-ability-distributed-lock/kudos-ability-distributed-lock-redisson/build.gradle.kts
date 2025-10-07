dependencies {
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-lock:kudos-ability-distributed-lock-common"))
    api(libs.redisson)

    testImplementation(project(":kudos-test:kudos-test-container"))
}