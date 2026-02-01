dependencies {
    api(project(":kudos-ams:kudos-ams-msg:kudos-ams-msg-core"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-provider"))
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-discovery:kudos-ability-distributed-discovery-nacos"))
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-config:kudos-ability-distributed-config-nacos"))
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-springmvc"))

    testImplementation(project(":kudos-test:kudos-test-container"))
}
