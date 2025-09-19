dependencies {
    api(project(":kudos-ability:kudos-ability-comm:kudos-ability-comm-common"))
    api("software.amazon.awssdk:sns:2.17.230")
    api("software.amazon.awssdk:apache-client:2.17.230")

    testImplementation(project(":kudos-test:kudos-test-common"))
}