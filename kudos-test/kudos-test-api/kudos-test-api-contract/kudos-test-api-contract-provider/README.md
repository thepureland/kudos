# kudos-test-api-contract-provider

API 契约测试 **provider 端**——验证 controller 满足契约。基于 Spring Cloud Contract 5.x +
JUnit 5 + MockMvc 模式。

## 内容

| 文件 | 角色 |
|---|---|
| `src/.../BaseContractTest` | provider 测试基类——`@SpringBootTest` + `@AutoConfigureMockMvc`，把 `MockMvc` 注入 `RestAssuredMockMvc`，让插件生成的测试可以走 rest-assured DSL |
| `test-src/.../UserController` | 样板 controller（仅测试期）：`GET /users/{id}` → `UserDto(id, "Tom")` |
| `src/contractTest/resources/contracts/user/get-user-by-id.yml` | 契约示例：路径 `/users/[0-9]+`，响应 200 + `{id, name}`，body 用 `by_regex` / `by_type` matcher 校验；spring-cloud-contract 插件从这里读 |

## 契约 → 测试代码的生成链

```
src/contractTest/resources/contracts/**.yml      ← 契约（YAML / Groovy DSL）
        │
        ▼  org.springframework.cloud.contract Gradle 插件（5.0.2）
build/generated-test-sources/contracts/...Test.java   ← 自动生成的 JUnit5 测试
        │ 继承 baseClassForTests = io.kudos.test.api.contract.provider.BaseContractTest
        ▼
        MockMvc → 真实 Controller → 校验响应 schema
```

业务侧用法：
1. 在自己的 provider 模块里 `api(project(":kudos-test:kudos-test-api:kudos-test-api-contract:kudos-test-api-contract-provider"))`。
2. 应用 `id("org.springframework.cloud.contract") version "5.0.2"`，
   `contracts { baseClassForTests.set("...") }` 指向自己继承 `BaseContractTest` 的子类（如
   果默认那个不够用——比如要塞 fixture / mock service，要复写）。
3. 把契约 `.yml` 放进 `contractsDslDir` 指定的目录。
4. `./gradlew :your-provider:test` 触发插件生成测试 + 执行。

## 已知问题

- ❗ **`BaseContractTest` 太薄**：只有 `mockMvc` 注入。业务侧通常还要：
  - 提供 mock 后端 service（`@MockBean`）
  - 准备 fixture 数据（不能简单 `@Sql`，因为 contract 测试由插件生成、没有 `@Sql` 注入点）
  - 关闭安全过滤器

  现在没有一处提供这些——业务侧基本要继承后再写。
- ❗ **`UserController` / `UserDto` 在 `test-src/`**——是本模块自己的 test sourceSet 内容，
  不会通过 `testImplementation(project(...))` 泄露到业务侧（除非显式应用
  `java-test-fixtures`）。但插件生成的 contract 测试会自动加载它们以校验
  `/users/{id}` 契约——如果业务侧把自家测试**直接**继承 `BaseContractTest`（而不是用插件
  生成路径），且自己的 `@SpringBootTest` 误扫到了 `io.kudos.test.api.contract.provider`
  包，就会冲突。常规用法下不会踩。
- ❗ **模块名 `provider` 与 `kudos-ability-cache-interservice-provider` 等"占位 ams
  脚手架 provider" 易混淆**——一个是测试基类（这里），一个是脚手架/示例工程。
- ❗ **`contracts { failOnNoContracts.set(true) }`**：业务侧引这个模块后若忘记放 yml，
  build 会直接失败——这是有意的（强制契约必须存在），但首次集成会踩坑。
