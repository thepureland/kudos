dependencies {
    api(project(":kudos-ability:kudos-ability-comm:kudos-ability-comm-common"))
    api(libs.amazon.awssdk.sns)
    api(libs.amazon.awssdk.apache.client)

    testImplementation(project(":kudos-test:kudos-test-container"))
}