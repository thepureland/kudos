dependencies {
    api(project(":kudos-ability:kudos-ability-comm:kudos-ability-comm-common"))
    api(libs.spring.boot.starter.mail)

    testImplementation(project(":kudos-test:kudos-test-container"))
}