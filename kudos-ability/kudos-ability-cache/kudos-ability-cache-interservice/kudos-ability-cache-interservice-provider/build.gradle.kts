dependencies {
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-common"))
    api("org.soul:soul-ability-cache-interservice-provider")

    testImplementation(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-client"))
    testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))
    testImplementation(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-local:kudos-ability-cache-local-caffeine"))
    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation("org.springframework.boot:spring-boot-starter-web")
}