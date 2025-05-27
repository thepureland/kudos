dependencies {
    api(project(":kudos-context"))
    api("org.soul:soul-ability-web-springmvc")
    api("javax.validation:validation-api:2.0.1.Final")

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation("org.springframework.boot:spring-boot-starter-jetty")
    testImplementation("org.springframework.boot:spring-boot-starter-undertow")
}

