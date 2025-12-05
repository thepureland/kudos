dependencies {
    implementation(project(":kudos-context"))
    api(project(":kudos-test:kudos-test-common"))
    api(libs.spring.boot.starter.webmvc.test)

    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner")
}
