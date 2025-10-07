dependencies {
    api(project(":kudos-context"))
    api(libs.spring.cloud.loadbalancer)
    api(libs.spring.cloud.starter.openfeign)

    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-discovery:kudos-ability-distributed-discovery-nacos"))
    testImplementation(libs.spring.boot.starter.web)
//    testImplementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
}