dependencies {
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-common"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-memdb:kudos-ability-data-memdb-redis"))
    api("org.soul:soul-ability-cache-remote-redis")

    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-local:kudos-ability-cache-local-caffeine"))
}