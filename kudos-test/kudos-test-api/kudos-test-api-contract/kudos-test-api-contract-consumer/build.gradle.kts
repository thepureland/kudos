dependencies {
    implementation(project(":kudos-context"))
    api(project(":kudos-test:kudos-test-common"))
    api(libs.spring.boot.starter.webmvc.test)

    testImplementation(libs.spring.cloud.starter.contract.stub.runner)
}

tasks.withType<Test>().configureEach {
    // This module intentionally ships a consumer-side contract-test template without @Test methods.
    failOnNoDiscoveredTests.set(false)
}
