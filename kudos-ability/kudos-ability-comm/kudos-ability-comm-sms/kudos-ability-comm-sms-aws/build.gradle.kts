dependencies {
    api(project(":kudos-ability:kudos-ability-comm:kudos-ability-comm-common"))
    api("software.amazon.awssdk:sns:2.33.13")
    api("software.amazon.awssdk:apache-client:2.33.13")

    testImplementation(project(":kudos-test:kudos-test-container"))
}