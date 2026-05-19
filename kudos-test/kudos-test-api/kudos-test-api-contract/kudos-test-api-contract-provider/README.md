# kudos-test-api-contract-provider

API 契约测试 **provider 端**——验证 controller 满足契约。

## 内容

| 文件 | 角色 |
|---|---|
| `BaseContractTest` | provider 端测试基类（持有 mock controller / mock service） |
| `UserController` （test-src 下） | 样板：用户契约的 controller mock |

## 使用

业务侧的 controller 测试继承 `BaseContractTest`，按 Spring Cloud Contract 风格验证请求 /
响应 schema。

## 已知限制

- ❗ 当前仅"用户契约"一个样板，没有泛化的 fixture / DSL
- ❗ 模块名 `provider` 易与 `kudos-ability-cache-interservice-provider`（脚手架占位）混淆
