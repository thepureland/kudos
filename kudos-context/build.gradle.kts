tasks.processResources {
    val kudosVersion = project.version.toString()
    val kotlinVersion = libs.versions.kotlin.get().toString()
    filesMatching("banner.txt") {
        filter { line ->
            line.replace($$"${kudosVersion}", kudosVersion)
                .replace($$"${kotlinVersion}", kotlinVersion)
        }
    }
}

dependencies {
    api(project(":kudos-base"))
    api(libs.spring.boot.starter.aop)
    api(libs.spring.tx)

    testImplementation(project(":kudos-test:kudos-test-common"))
}