plugins {
    alias(libs.plugins.ktor) // Pulls in the BOM automatically.
}

dependencies {
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-common"))

    // Configuration files support YAML.
    api(libs.ktor.server.core)
//    api("io.ktor:ktor-server-test-host-jvm")
    api(libs.ktor.server.config.yaml)
    api(libs.ktor.server.content.negotiation)
    api(libs.ktor.server.status.pages)
    api(libs.ktor.server.websockets)

    // Logging
//    api("io.ktor:ktor-server-call-logging-jvm")
//    // CORS plugin example
//    api("io.ktor:ktor-server-cors-jvm")
//    // Server-Sent Events (SSE)
//    api("io.ktor:ktor-server-sse-jvm")
//    // CSRF protection
//    api("io.ktor:ktor-server-csrf-jvm")

//    api("ch.qos.logback:logback-classic:1.5.18")
//    api("commons-logging:commons-logging:1.3.5")


    // Test dependencies
//    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:${libs.versions.kotlin.get()}")
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.server.netty)
    testImplementation(libs.ktor.server.jetty.jakarta)
    testImplementation(libs.ktor.server.tomcat.jakarta)
    testImplementation(libs.ktor.server.cio)

    testImplementation(project(":kudos-test:kudos-test-common"))
}

application {
    // Sets the main class for the application plugin so that the packaged jar's Manifest includes Main-Class: io.ktor.server.netty.EngineMain.
    mainClass.set("io.ktor.server.netty.EngineMain")
}
