dependencies {
    api(project(":kudos-context"))
    api(libs.spring.web)
    api(libs.servlet.api)

    testImplementation(project(":kudos-test:kudos-test-common"))
}