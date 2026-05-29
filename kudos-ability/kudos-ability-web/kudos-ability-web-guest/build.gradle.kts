dependencies {
    api(project(":kudos-context"))

    // Provides `HttpServletRequest.getRemoteIp()` (X-Forwarded-For aware) used by the default
    // GuestAccessUniqueKey, plus the broader servlet stack the filter depends on.
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-springmvc"))

    // The default GuestAccessStore stashes one Redis hash per active visitor; depending on the
    // memdb-redis module here is what wires the named-RedisTemplate lookup the store performs.
    // Apps wanting an in-memory / JDBC-backed store can declare their own IGuestAccessStore bean
    // and the @ConditionalOnMissingBean default will step aside.
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-memdb:kudos-ability-data-memdb-redis"))

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.jetty)
    testImplementation(libs.spring.boot.starter.data.redis)
}
