dependencies {
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-notify:kudos-ability-distributed-notify-common"))
    api(libs.spring.context.support)

    testImplementation(project(":kudos-test:kudos-test-container"))
}