dependencies {
    api(project(":kudos-base"))
    api(libs.spring.boot.starter.aop)
    api(libs.spring.tx)
    api(libs.spring.boot.properties.migrator)

    testImplementation(project(":kudos-test:kudos-test-common"))
}