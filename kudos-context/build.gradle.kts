dependencies {
    api(project(":kudos-base"))
    api("org.springframework.boot:spring-boot-starter-aop")
    api("org.springframework:spring-tx")

    testImplementation(project(":kudos-test:kudos-test-common"))
}