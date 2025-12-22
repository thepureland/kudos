dependencies {
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-vectordb:kudos-ability-data-vectordb-common"))
    api(libs.spring.ai.milvus.store)

    testImplementation(project(":kudos-test:kudos-test-container"))
}