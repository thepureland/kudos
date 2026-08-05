dependencies {
    api(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-core"))
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-springmvc"))

    // @WebAudit on the mutating endpoints. Authorization changes are exactly the kind of change that
    // has to be answerable after the fact: who granted what, to whom, when.
    api(project(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-common"))
    


    testImplementation(project(":kudos-test:kudos-test-container"))
    // The shadowDemo launcher boots the real application against the test containers, so the test
    // classpath needs the H2 driver the core module only holds in its own test scope.
    testImplementation(libs.h2database.h2)
    // Ktorm resolves its SQL dialect off the classpath via ServiceLoader; without one, the first
    // paginated query dies with "Pagination is not supported in Standard SQL". Same pairing the
    // core module's tests use: the H2 container speaks enough PostgreSQL for this dialect.
    testImplementation(libs.ktorm.support.postgresql)
}

// Runs the real admin backend with shadow mode on, against the same containers the tests use.
// Lives on the test classpath on purpose: it is a dev tool, not a deployment artifact — see
// ShadowModeDemoLauncher for the reasoning.
tasks.register<JavaExec>("shadowDemo") {
    group = "application"
    description = "Boot the auth admin backend on :8080 with enforcement + row-scope shadow mode lit"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("io.kudos.ms.auth.api.admin.dev.ShadowModeDemoLauncherKt")
}
