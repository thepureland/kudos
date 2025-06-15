dependencies {
    api(project(":kudos-context"))
    api("org.soul:soul-ability-comm-websocket-netty")

    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation("org.java-websocket:Java-WebSocket:1.5.3")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
}