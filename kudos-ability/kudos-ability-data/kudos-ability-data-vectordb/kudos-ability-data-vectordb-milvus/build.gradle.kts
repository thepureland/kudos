dependencies {
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-vectordb:kudos-ability-data-vectordb-common"))
    api(libs.spring.ai.starter.vector.store.milvus)

    testImplementation(project(":kudos-test:kudos-test-container"))
}