dependencies {
    api(project(":kudos-context")) {
        exclude("io.kudos", "kudos-test-common")
    }
    api("org.springframework.boot:spring-boot-starter-test")
}