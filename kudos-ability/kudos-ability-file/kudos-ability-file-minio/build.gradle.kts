dependencies {
    api(project(":kudos-ability:kudos-ability-file:kudos-ability-file-common"))
    api(libs.minio)

    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation(libs.minio.admin)
}