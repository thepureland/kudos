dependencies {
    api(project(":kudos-context"))

    // Declared explicitly rather than relied on transitively (kudos-base exposes it via `api`):
    // this module's public API includes kotlinx.coroutines types — every handler hook is `suspend`,
    // and KudosContextThreadElement implements ThreadContextElement — so the dependency is part of
    // its contract, not an accident of what an upstream module happens to re-export today.
    api(libs.kotlinxCoroutines)

    // Optional Redis-backed distributed broadcasting (see distributed/redis/). compileOnly so
    // single-instance deployments do not pull Spring Data Redis through this module; apps that
    // want cross-process broadcasting add Spring Data Redis themselves.
    compileOnly(libs.spring.boot.starter.data.redis)

    testImplementation(project(":kudos-test:kudos-test-common"))
    // RedisWebSocketBroadcastChannel is compiled against compileOnly spring-data-redis; unit tests
    // need it on the test runtime classpath (collaborators are mocked, no real Redis involved).
    testImplementation(libs.spring.boot.starter.data.redis)
}
