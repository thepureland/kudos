# kudos-test-api-contract-consumer

API 契约测试 **consumer 端**——验证 Feign client 与契约对齐。

## 内容

| 文件 | 角色 |
|---|---|
| `UserClientTest` （test-src 下） | 样板：用户契约的 Feign client 测试 |

## 使用

业务侧 Feign client 测试可参照样板：mock 远端服务 + 校验 client 调用结果符合契约。

## 已知限制

- ❗ 当前只有样板代码，没有泛化的 fixture / DSL
- ❗ 与 provider 端是对偶关系——契约定义如果在两侧不一致，编译期不会报错，需运行时跑通才能发现
