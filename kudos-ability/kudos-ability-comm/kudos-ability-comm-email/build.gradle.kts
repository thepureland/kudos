dependencies {
    api(project(":kudos-ability:kudos-ability-comm:kudos-ability-comm-common"))
    api("org.springframework.boot:spring-boot-starter-mail")
    testImplementation(project(":kudos-test:kudos-test-container"))
}