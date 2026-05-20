package io.kudos.test.api.contract.consumer

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties

/**
 * Consumer 端契约测试样板：起 Stub Runner、把 WireMock 监听到 8081，业务侧自己注入
 * Feign / RestTemplate / WebClient 后断言调用结果与 stub 一致。
 *
 * 业务侧用法：复制本类，把 `ids` 改成真实 provider 坐标，注入自己的 client + 写 @Test。
 */
@SpringBootTest
@AutoConfigureStubRunner(
    // groupId:artifactId:version:classifier:port — 业务侧替换为真实 provider 坐标
    ids = ["com.example:user-service:+:stubs:8081"],
    // REMOTE 从 Maven 仓库拉 stubs jar；本地开发 provider 未发版时切 LOCAL（读 ~/.m2）
    stubsMode = StubRunnerProperties.StubsMode.REMOTE
)
class UserClientTest
