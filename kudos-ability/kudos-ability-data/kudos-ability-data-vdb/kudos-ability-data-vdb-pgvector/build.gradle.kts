dependencies {
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-vdb:kudos-ability-data-vdb-common"))
//    api(project(":kudos-ability-data-vectordb-commonty:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc"))
    api(libs.spring.ai.starter.vector.store.pgvector)
    api(libs.postgresql)

    testImplementation(project(":kudos-test:kudos-test-container"))
}