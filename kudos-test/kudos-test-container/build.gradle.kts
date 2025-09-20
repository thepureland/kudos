dependencies {
    implementation(project(":kudos-context"))
    api(project(":kudos-test:kudos-test-common"))
    api("org.testcontainers:testcontainers:1.21.0") {
        exclude("junit", "junit")
    }
    api("org.testcontainers:kafka:1.21.0")
    api("org.testcontainers:junit-jupiter:1.21.0")
    api("org.wiremock:wiremock:3.13.1")
}