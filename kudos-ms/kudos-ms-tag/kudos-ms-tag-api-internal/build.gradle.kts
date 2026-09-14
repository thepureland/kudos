dependencies {
    api(project(":kudos-ms:kudos-ms-tag:kudos-ms-tag-core"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-provider"))
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-discovery:kudos-ability-distributed-discovery-nacos"))
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-config:kudos-ability-distributed-config-nacos"))
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-springmvc"))
    runtimeOnly(libs.h2database.h2)

    testImplementation(project(":kudos-ms:kudos-ms-tag:kudos-ms-tag-client"))
    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(libs.spring.boot.starter.web)
    testImplementation(libs.kotlin.test.junit5)
}
