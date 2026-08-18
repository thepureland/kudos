plugins {
    alias(libs.plugins.ktor) // Brings the Ktor BOM; consistent with kudos-ability-web-ktor, no need to declare a version on test deps.
}

dependencies {
    // The engine-neutral half of this ability: session registry, broadcaster, handler SPI, admission
    // interceptors, codec SPI, KudosContext bridge and distributed delivery. `api` because a consumer
    // of this module inevitably names those types — `kudosWebSocket(...)` takes a KudosWebSocketRegistry
    // and an IKudosWebSocketHandler, both of which live there.
    api(project(":kudos-ability:kudos-ability-comm:kudos-ability-comm-websocket:kudos-ability-comm-websocket-common"))

    api(libs.ktor.server.websockets)

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(libs.ktor.server.test.host)
}
