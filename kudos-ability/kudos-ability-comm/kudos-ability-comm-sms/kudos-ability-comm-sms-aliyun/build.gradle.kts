dependencies {
    api(project(":kudos-ability:kudos-ability-comm:kudos-ability-comm-common"))
    api(libs.alibaba.cloud.dysmsapi)

    testImplementation(project(":kudos-test:kudos-test-container"))
}