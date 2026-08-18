// No `alias(libs.plugins.ktor)` here. That plugin exists to package a runnable Ktor application: it
// contributes a `shadowJar` task that fails the build with "The main class must be specified" unless
// an `application { mainClass }` is declared — which a library module has none to declare, and
// inventing one would stamp a bogus Main-Class into a jar nobody runs. (kudos-ability-web-ktor does
// declare EngineMain, which is why it survives `publishToMavenLocal` and this module did not.)
//
// The only thing this module wanted from it was the Ktor BOM, and the root build already adds
// `platform(libs.ktor.bom)` to every Kotlin module — including test configurations, since
// `testImplementation` extends `implementation`. So the versionless `libs.ktor.*` coordinates below
// still resolve.


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
