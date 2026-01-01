dependencies {
    api(project(":kudos-context"))
    api(libs.spring.ai.vector.store)

    testImplementation(project(":kudos-test:kudos-test-common"))
}