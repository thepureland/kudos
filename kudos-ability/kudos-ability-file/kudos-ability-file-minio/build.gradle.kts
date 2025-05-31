dependencies {
    api(project(":kudos-context"))
    api("org.soul:soul-ability-file-minio")

    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation("io.minio:minio-admin:8.4.3")
}