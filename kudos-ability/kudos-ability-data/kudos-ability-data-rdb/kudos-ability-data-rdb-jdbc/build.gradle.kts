dependencies {
    api(project(":kudos-context"))
    api(libs.spring.boot.starter.jdbc)
    api(libs.baomidou.dynamic.datasource.starter)

    // HikariCP Micrometer integration is optional: when Actuator (or any MeterRegistry) is on the
    // classpath, [HikariDataSourceMeterInitEvent] takes over from baomidou's default init event and
    // exposes pool metrics. Stays compileOnly so apps without observability don't pull the dep.
    compileOnly(libs.micrometer.core)

    // Cache-clean SPI is optional: when kudos-ability-cache-common is on the classpath,
    // [DataSourceClearListener] subscribes to the sys-datasource cache so cache invalidation
    // drives a live refresh of the dynamic data-source registry without restart. compileOnly so
    // apps without the cache module are not forced to pull it in.
    compileOnly(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-common"))

    testImplementation(libs.micrometer.core)
    testImplementation(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-common"))
    testImplementation(libs.h2database.h2)
    testImplementation(libs.postgresql)
    testImplementation(project(":kudos-test:kudos-test-container"))
}