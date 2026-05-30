dependencies {
    api(project(":kudos-context"))
    api(project(":kudos-test:kudos-test-common"))
    api(libs.testcontainers) {
        exclude("junit", "junit")
    }
    api(libs.testcontainers.kafka)
    api(libs.testcontainers.junit.jupiter)
    api(libs.testcontainers.mongodb)
    api(libs.testcontainers.clickhouse)
    api(libs.testcontainers.influxdb)

    // On macOS, starting Lettuce/Netty without the native DNS resolver falls back to the JVM default,
    // which may affect /etc/hosts, Bonjour, etc. Add the proper classifier for the current host architecture;
    // not pulled in on Linux/Windows.
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        val arch = System.getProperty("os.arch").lowercase()
        val classifier = if (arch == "aarch64" || arch == "arm64") "osx-aarch_64" else "osx-x86_64"
        api(variantOf(libs.netty.resolver.dns.native.macos) { classifier(classifier) })
    }
}