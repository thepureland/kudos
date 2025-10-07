dependencies {
    api(project(":kudos-base"))
    api(libs.spring.boot.starter.aop)
    api(libs.spring.tx)

    testImplementation(project(":kudos-test:kudos-test-common"))
}