dependencies {
    api(project(":kudos-base"))
    api("org.springframework.boot:spring-boot-starter-aop")

    testImplementation(project(":kudos-test:kudos-test-common"))
}