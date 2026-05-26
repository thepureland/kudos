// The code in this file is a convention plugin - a Gradle mechanism for sharing reusable build logic.
// `buildSrc` is a Gradle-recognized directory and every plugin there will be easily available in the rest of the build.
package buildsrc.convention

import org.gradle.api.tasks.testing.logging.TestLogEvent

// Note: this is a "convention plugin" in buildSrc, but the repo root build.gradle.kts currently does not import it
// via `apply(plugin = ...)`, so this file is effectively inactive. The unified configuration related to the kotlin-jvm
// plugin and Test tasks (JUnitPlatform, test logging, kotlin-spring allopen, Mockito javaagent, etc.) is currently
// applied directly inside the `subprojects { ... }` block in the root build.gradle.kts.
// This file is kept for reuse if we later truly switch back to the convention plugin approach.
plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin in JVM projects.
    kotlin("jvm")
}

kotlin {
    // Use a specific Java version to make it easier to work in different environments.
    jvmToolchain(25)
}

tasks.withType<Test>().configureEach {
    // Configure all test Gradle tasks to use JUnitPlatform.
    useJUnitPlatform()

    // Log information about all test results, not only the failed ones.
    testLogging {
        events(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED
        )
    }
}
