dependencies {
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-common"))
    api(libs.alibaba.cloud.starter.stream.rocketmq)

    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))
    testImplementation(libs.h2database.h2)
    testImplementation(libs.spring.boot.starter.web)
}
