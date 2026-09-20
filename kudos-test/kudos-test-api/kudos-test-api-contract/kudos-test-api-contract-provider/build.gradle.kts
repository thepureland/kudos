plugins {
    java
    alias(libs.plugins.spring.cloud.contract)
}

dependencies {
    implementation(project(":kudos-context"))
    api(project(":kudos-test:kudos-test-common"))
    api(libs.spring.cloud.starter.contract.verifier)
    api(libs.spring.boot.starter.webmvc.test)

    testImplementation(libs.spring.boot.starter.web)
}

// Spring Cloud Contract plugin configuration
contracts {
    // Contract files directory
//    contractsDslDir.set(file("test-resources/contracts"))
    contractsDslDir.set(file("$projectDir/src/contractTest/resources/contracts"))


    // Base class used by generated tests
    baseClassForTests.set("io.kudos.test.api.contract.provider.BaseContractTest")

    testFramework.set(org.springframework.cloud.contract.verifier.config.TestFramework.JUNIT5)
    testMode.set(org.springframework.cloud.contract.verifier.config.TestMode.MOCKMVC)

    // Whether to generate stubs
    failOnNoContracts.set(true)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
