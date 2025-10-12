dependencies {
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-common"))

    api(libs.spring.boot.starter.web)
    api(libs.commons.fileupload)
    api(libs.spring.session.core)
    api(libs.spring.session.data.redis)

    testImplementation(project(":kudos-test:kudos-test-common"))
}
