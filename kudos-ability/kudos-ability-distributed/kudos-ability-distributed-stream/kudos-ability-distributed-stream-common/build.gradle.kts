dependencies {
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm")) {
        exclude(group = "com.fasterxml.jackson.module", module = "jackson-module-kotlin")
    }
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-flyway"))
    api(libs.spring.cloud.stream)
    // Spring Boot 4 使用 Jackson 3 (tools.jackson)，Spring Cloud Function 需要 Jackson 3 的 Kotlin 模块
    api(libs.jackson.module.kotlin)
}