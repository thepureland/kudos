dependencies {
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-common"))
    api("org.soul:soul-ability-cache-interservice-client")
    compileOnly(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))

    testImplementation(project(":kudos-test:kudos-test-container"))
}