dependencies {
    api(project(":kudos-context"))
    api(project(":kudos-test:kudos-test-common"))
    api(libs.testcontainers) {
        exclude("junit", "junit")
    }
    api(libs.testcontainers.kafka)
    api(libs.testcontainers.junit.jupiter)

    // macOS 启动 Lettuce/Netty 时缺少原生 DNS 解析器会 fallback 到 JVM 默认解析，
    // 可能影响 /etc/hosts、Bonjour 等场景。按当前主机架构带上对应分类器，Linux/Windows 上不会引入。
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        val arch = System.getProperty("os.arch").lowercase()
        val classifier = if (arch == "aarch64" || arch == "arm64") "osx-aarch_64" else "osx-x86_64"
        api(variantOf(libs.netty.resolver.dns.native.macos) { classifier(classifier) })
    }
}