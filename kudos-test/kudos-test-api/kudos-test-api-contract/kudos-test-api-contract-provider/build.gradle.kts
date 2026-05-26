plugins {
    java
    id("org.springframework.cloud.contract") version "5.0.2"
}

dependencies {
    implementation(project(":kudos-context"))
    api(project(":kudos-test:kudos-test-common"))
    api("org.springframework.cloud:spring-cloud-starter-contract-verifier")
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
