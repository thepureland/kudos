# kudos-test-api-contract-consumer

API 契约测试 **consumer 端**——验证 Feign/HTTP client 调用与 provider 发布的 stub 对齐。
基于 Spring Cloud Contract Stub Runner。

## 内容

| 文件 | 角色 |
|---|---|
| `test-src/.../UserClientTest` | 样板：`@SpringBootTest` + `@AutoConfigureStubRunner(ids = ["com.example:user-service:+:stubs:8081"])` |

样板**只起 stub runner 容器、不含 @Test 断言**——业务侧复制类后注入自己的 client + 写
断言才能真正校验。`ids` 里的 `com.example:user-service` 是占位坐标，必须改成自己 provider
真实坐标。

## Consumer 端流程

```
1. Provider 在自己的 jar 里发布 stubs jar：<groupId>:<artifactId>:<version>:stubs
2. AutoConfigureStubRunner 从 Maven repo（REMOTE）或本地 m2（LOCAL）下载 stubs
3. 启 WireMock 在指定端口（这里 8081）按 stubs 重放请求
4. Feign / RestTemplate / WebClient 调 http://localhost:8081/... → 拿到 stub 响应
5. 测试断言 client 解析正确
```

`stubsMode`：
- `REMOTE` —— 从 Maven 仓库拉 stubs jar（CI 默认）
- `LOCAL` —— 用本地 `~/.m2/repository` 里 install 好的 stubs（本地开发 + provider 还没
  发版时）
- `CLASSPATH` —— 直接从 classpath 找 stubs

## 与 provider 端的对偶关系

```
provider 模块 (.../contract-provider)         consumer 模块 (这里)
├── 契约 yml ────────发布 stubs jar─────────► AutoConfigureStubRunner 下载
├── BaseContractTest 验证 controller          ▲
└── 插件生成 *Test.java                       │
                                       Feign client 调本地 wiremock → 校验
```

**契约定义如果在两侧不一致，编译期不会报错**——只能靠运行时跑通双侧测试才能发现。
provider 改了响应字段而不发版 stubs，consumer 拿不到；consumer 假设了新字段，REMOTE
stubs 是旧的，测试就会暴露。

## 已知限制

- ❗ **样板不含 @Test**——只跑上下文启动 + stub runner，不验证任何业务断言。业务侧必须
  补 client 注入和 `@Test` 才有意义。
- ❗ **`ids = ["com.example:user-service:+:stubs:8081"]`** 是占位 group/artifact——业务侧
  必须改成自己 provider 真实坐标，否则 stub runner 会从 maven 仓库找不到。
- ❗ **`stubsMode = REMOTE`**：本地开发时如果 provider 还没 `mvn install` 过 stubs jar，
  会报"找不到 stubs"。建议本地切 `LOCAL`、CI 用 `REMOTE`。
- ❗ **端口 8081 hardcoded**——并发跑多 consumer 测试会冲突；多个 stub 服务时记得各自分
  不同端口。
- ❗ **没有泛化的 fixture / DSL**——业务侧每个 consumer 都要从零拷贝这套样板。
- ❗ 模块名 `consumer` 与 `kudos-ability-cache-interservice-consumer`（脚手架占位）易
  混淆。
