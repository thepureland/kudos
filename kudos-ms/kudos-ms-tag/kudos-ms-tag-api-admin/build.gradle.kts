dependencies {
    api(project(":kudos-ms:kudos-ms-tag:kudos-ms-tag-core"))
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-springmvc"))
    api(project(":kudos-ability:kudos-ability-security:kudos-ability-security-enforcement"))

    testImplementation(libs.kotlin.test.junit5)
}
