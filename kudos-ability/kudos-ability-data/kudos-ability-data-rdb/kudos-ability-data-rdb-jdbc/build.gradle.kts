dependencies {
    api(project(":kudos-context"))
    api(libs.spring.boot.starter.jdbc)
    api(libs.baomidou.dynamic.datasource.starter)
//    api("io.micrometer:micrometer-core")

    testImplementation(libs.h2database.h2)
    testImplementation(project(":kudos-test:kudos-test-common"))
}