dependencies {
    api(project(":kudos-context"))
    api("org.springframework.boot:spring-boot-starter-jdbc")
    api("com.baomidou:dynamic-datasource-spring-boot3-starter:4.2.0")
//    api("io.micrometer:micrometer-core")

    testImplementation("com.h2database:h2:2.3.232")
    testImplementation(project(":kudos-test:kudos-test-common"))
}