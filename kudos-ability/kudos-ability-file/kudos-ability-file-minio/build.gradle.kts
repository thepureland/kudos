dependencies {
    api(project(":kudos-ability:kudos-ability-file:kudos-ability-file-common"))
    api("io.minio:minio:8.5.17")

    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation("io.minio:minio-admin:8.5.17")
}