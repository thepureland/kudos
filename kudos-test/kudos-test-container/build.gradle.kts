dependencies {
    implementation(project(":kudos-context"))
    api(project(":kudos-test:kudos-test-common"))
    api(libs.testcontainers) {
        exclude("junit", "junit")
    }
    api(libs.testcontainers.kafka)
    api(libs.testcontainers.junit.jupiter)
}