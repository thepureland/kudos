# kudos-test-api

API 契约测试基类——基于 Spring Cloud Contract 风格的双侧测试。

## 子模块

```
kudos-test-api
└── kudos-test-api-contract
    ├── kudos-test-api-contract-provider
    │   └── BaseContractTest        # provider 端基类（含 mock controller 桩）
    └── kudos-test-api-contract-consumer
        └── UserClientTest          # consumer 端示例
```

| 子模块 | 角色 |
|---|---|
| `contract-provider` | provider 端测试基类——验证 controller 满足契约 |
| `contract-consumer` | consumer 端测试样板——验证 Feign client 与契约对齐 |

## 已知限制

- ❗ 当前只有"用户契约"的样板，没有泛化基类——业务侧若要做契约测试需要参照该样本自己写
- ❗ 模块名 `contract-provider` 与 `kudos-ability-cache-interservice-provider` 等"占位 ams
  脚手架 provider"重名易混淆——一个是测试基类，一个是脚手架
