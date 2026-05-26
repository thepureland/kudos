package io.kudos.test.api.contract.consumer

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties

/**
 * Consumer-side contract test template: starts Stub Runner with WireMock listening on 8081.
 * The business side injects its own Feign / RestTemplate / WebClient and asserts that call
 * results match the stub.
 *
 * Business-side usage: copy this class, replace `ids` with the real provider coordinates,
 * inject your own client, and add @Test methods.
 */
@SpringBootTest
@AutoConfigureStubRunner(
    // groupId:artifactId:version:classifier:port — business side replaces with real provider coordinates
    ids = ["com.example:user-service:+:stubs:8081"],
    // REMOTE pulls the stubs jar from a Maven repository; switch to LOCAL (reads ~/.m2) when the provider has not been released yet during local development
    stubsMode = StubRunnerProperties.StubsMode.REMOTE
)
class UserClientTest
