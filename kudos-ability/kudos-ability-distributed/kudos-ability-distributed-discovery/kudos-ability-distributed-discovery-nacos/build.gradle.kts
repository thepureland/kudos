dependencies {
    api(project(":kudos-context"))
    api("org.soul:soul-ability-distributed-discovery-nacos")

    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation("org.springframework.boot:spring-boot-starter-web")
}