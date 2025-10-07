dependencies {
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-common"))
    api(libs.github.caffeine)

    testImplementation(project(":kudos-test:kudos-test-common"))
}