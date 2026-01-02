dependencies {
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-vdb:kudos-ability-data-vdb-common"))
    api(libs.spring.ai.starter.vector.store.milvus)

    testImplementation(libs.spring.ai.starter.model.ollama)
    testImplementation(project(":kudos-test:kudos-test-container"))
}