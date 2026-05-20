# kudos-test-api-contract

API 契约测试——provider / consumer 双侧的测试基类与样板，基于 Spring Cloud Contract 5.x。

## 子模块

| 子目录 | 角色 |
|---|---|
| [`kudos-test-api-contract-provider`](kudos-test-api-contract-provider/README.md) | provider 端：契约 yml + Gradle 插件生成 controller 测试，`BaseContractTest` 为生成测试提供 MockMvc 基类 |
| [`kudos-test-api-contract-consumer`](kudos-test-api-contract-consumer/README.md) | consumer 端：用 Stub Runner 起 WireMock 重放 provider stubs，校验 client 端解析 |

## 双侧契约对齐机制

```
契约 yml（provider 模块的 contractsDslDir 下）
    │
    ├──Gradle 插件─→ provider 端：生成 *Test.java，继承 BaseContractTest，跑 MockMvc 校验
    │
    └──发布 stubs jar（<gav>:stubs classifier）
                │
                └──Stub Runner─→ consumer 端：启 WireMock(8081)，client 调本地 stub
```

只有**同一份契约**喂给两侧、且都跑通才算"对齐"——任何一侧改了契约而另一侧没同步，要等
运行时测试才暴露（编译期不报）。

## 业务侧接入步骤

**Provider 模块**：
1. `api(project(":kudos-test:...:kudos-test-api-contract-provider"))`
2. 应用 `org.springframework.cloud.contract` 插件，`baseClassForTests = "<继承自
   BaseContractTest 的类>"`
3. 把契约 yml 放到 `contractsDslDir` 指定的目录
4. `gradle publish` 发 stubs jar 到内部 Maven 仓库

**Consumer 模块**：
1. `testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner")`
2. 测试上加 `@AutoConfigureStubRunner(ids = ["<provider gav>:stubs:<port>"], stubsMode
   = LOCAL/REMOTE)`
3. 用 Feign / RestTemplate / WebClient 调 `http://localhost:<port>/...` 断言

参考样板：本目录两个子模块里的 `UserController` + `UserClientTest` + `get-user-by-id.yml`。

## 已知限制

- ❗ **目前两侧都只有"用户契约"一个样板**——没有泛化的 fixture / DSL。业务侧基本是
  "复制样板再改"。
- ❗ **consumer 样板没有 @Test**——只起 stub runner，不验断言（详见 consumer 子模块
  README）。
- ❗ **`BaseContractTest` 太薄**——只有 `mockMvc` 注入，业务侧通常还要补 mock service /
  fixture / 安全过滤器关闭。
