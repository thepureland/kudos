dependencies {
    // The engine-neutral half of this ability: session registry, broadcaster, handler SPI, admission
    // interceptors, codec SPI, KudosContext bridge and distributed delivery — shared verbatim with
    // kudos-ability-comm-websocket-ktor. `api` because a consumer inevitably names those types: the
    // endpoints it registers are driven by an IKudosWebSocketHandler and fan out through an
    // IWebSocketBroadcaster.
    api(project(":kudos-ability:kudos-ability-comm:kudos-ability-comm-websocket:kudos-ability-comm-websocket-common"))

    // Spring's own WebSocket support: WebSocketHandler, HandshakeInterceptor, WebSocketConfigurer.
    api(libs.spring.websocket)

    // A Spring WebSocket endpoint only exists inside a servlet web application — `registerWebSocketHandlers`
    // is Spring MVC infrastructure and the upgrade is performed by the servlet container. Declaring the web
    // starter states that requirement instead of leaving it to whichever module happens to drag it in.
    //
    // Deliberately NOT a dependency on kudos-ability-web-springmvc: this module needs the servlet + MVC
    // runtime, not that module's controllers, filters and exception advice. Applications combining the two
    // is the common case, but coupling them would force every WebSocket-only service to inherit an HTTP
    // stack it never registers a controller against.
    api(libs.spring.boot.starter.web)

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(libs.spring.boot.starter.webmvc.test)
}
