dependencies {
    api(project(":kudos-context"))

    // Spring Data MongoDB starter — provides MongoClient + MongoTemplate auto-config wired off
    // `spring.data.mongodb.*`. The kudos module rides on those Spring properties directly rather
    // than introducing a parallel kudos namespace, so existing Spring docs / IDE auto-complete
    // remain accurate.
    api(libs.spring.boot.starter.data.mongodb)

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(project(":kudos-test:kudos-test-container"))
}
