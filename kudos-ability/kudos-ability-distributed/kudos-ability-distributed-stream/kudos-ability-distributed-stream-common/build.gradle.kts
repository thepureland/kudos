dependencies {
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm")) {
        exclude(group = "com.fasterxml.jackson.module", module = "jackson-module-kotlin")
    }
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-flyway"))
    api(libs.spring.cloud.stream)
    // Spring Boot 4 uses Jackson 3 (tools.jackson); Spring Cloud Function needs the Jackson 3 Kotlin module
    api(libs.jackson.module.kotlin)

    testImplementation(project(":kudos-test:kudos-test-common"))
}
