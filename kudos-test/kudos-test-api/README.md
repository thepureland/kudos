# kudos-test-api

API 测试支持。当前只有"契约测试"一个分支，基于 Spring Cloud Contract 5.x 做 provider /
consumer 双侧验证。

## 子模块

```
kudos-test-api
└── kudos-test-api-contract          双侧契约测试入口
    ├── kudos-test-api-contract-provider
    │   ├── BaseContractTest         provider 测试基类（MockMvc + RestAssured）
    │   ├── UserController            样板 controller（test-src）
    │   └── contracts/**.yml          契约定义
    └── kudos-test-api-contract-consumer
        └── UserClientTest            consumer 端样板（AutoConfigureStubRunner）
```

| 子模块 | 角色 |
|---|---|
| [`kudos-test-api-contract`](kudos-test-api-contract/README.md) | 契约测试总目录（含 provider / consumer 两个子目录） |

## 为什么独立成 `kudos-test-api`

`kudos-test-api` 目前只装了 `kudos-test-api-contract`，但留出了独立顶层目录——是为了将来
接其它 API 测试（如 OpenAPI schema 校验、GraphQL 契约、protobuf compat）时有处可放，不
让 `kudos-test-api-contract` 顶上"代表所有 API 测试"的位置。

## 已知限制

- ❗ **当前只覆盖 REST 契约**——没有 OpenAPI / GraphQL / gRPC / 事件契约测试。
- ❗ **样板只有"用户契约"一个**——没有泛化基类，业务侧做契约测试要参照样板自己写
  （fixture、mock、安全过滤器关闭等都要重新搭）。
- ❗ **`provider` / `consumer` 子模块名与 `kudos-ability-cache-interservice-provider /
  -consumer` 等"占位 ams 脚手架 provider/consumer"重名**——一个是测试基类、一个是脚手
  架工程，IDE 里搜 `provider` 容易点错。

## 改进建议（自动分析 2026-06-11）

### 可维护性 / 对外接口
- `kudos-test-api-contract-consumer/test-src/.../UserClientTest.kt`：样板类硬编码占位坐标
  `com.example:user-service` 与固定 stub 端口 8081，且位于 test-src（会随测试任务编译加载，
  只是因为没有 @Test 方法才不失败）。建议加 `@Disabled("模板，业务侧复制后使用")` 或挪到
  文档示例，固定端口改为 `stubsPerConsumer`/动态端口，避免与本机其它进程冲突。
- `kudos-test-api-contract-provider/src/.../BaseContractTest.kt`：`RestAssuredMockMvc.mockMvc(...)`
  设置的是**全局静态**状态，同一 JVM 内多个上下文并行跑契约测试会互相覆盖；建议在 KDoc 中
  注明"契约测试需串行"或改用 `webAppContextSetup` per-instance 形式。

### 功能补充
- 仅覆盖 REST 契约（已知限制亦提及）。最值得先补的是：可参数化 stub 坐标/端口的泛化
  consumer 基类，以及 provider 侧"关安全过滤器 + 通用异常映射"的开箱配置，降低业务侧
  复制样板的成本。
