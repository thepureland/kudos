# kudos-test-common

最基础的测试支持：`@EnableKudosTest` 元注解 + 一个 Spring `TestExecutionListener`，把
kudos 的最小 Spring 上下文接到 Spring Boot Test 的测试上下文缓存里。

## 内容

| 文件 | 角色 |
|---|---|
| `init/EnableKudosTest` | 元注解，组合 `@SpringBootTest` + `classes=[TestApplication::class]` + `webEnvironment=MOCK` |
| `init/TestApplication` | 标 `@EnableKudos` 的最小启动类——拉起 kudos `IComponentInitializer` 链 |
| `init/SpringKitTestContextListener` | 把当前测试上下文同步进 `SpringKit.applicationContext` 静态字段 |
| `resources/META-INF/spring.factories` | 注册上面这个 listener 到 Spring Test 框架 |

## `@EnableKudosTest` 怎么工作

它 itself 是被 `@SpringBootTest` 标注的元注解，Spring `@AliasFor` 把它的几个参数透传到
内层 `@SpringBootTest`：

| `@EnableKudosTest` 参数 | 透传到 `@SpringBootTest` | 默认 |
|---|---|---|
| `classes` | `classes` | `[TestApplication::class]` |
| `webEnvironment` | `webEnvironment` | `WebEnvironment.MOCK` |
| `properties` | `properties` | `[]` |

要排除特定的 `IComponentInitializer`：在 `@EnableKudos(exclusions = [...])` 上加，作用在
被注解的启动类（`TestApplication` 或自定义启动类）。`@EnableKudosTest` 不直接承担这个
责任——`ComponentInitializerSelector` 只读启动类上的 `@EnableKudos.exclusions`。

```kotlin
@EnableKudosTest(
    classes = [MyApp::class],                          // 替换默认 TestApplication
    webEnvironment = WebEnvironment.RANDOM_PORT,        // 要真起 web server
    properties = ["my.flag=true"],
)
class MyServiceTest { ... }
```

`@Inherited` + `@MustBeDocumented`——子类自动继承，KDoc 会带上注解。

## 关键修复：SpringKit 与上下文缓存复用

`SpringKitTestContextListener.prepareTestInstance()` 在每次准备测试实例时把
`SpringKit.applicationContext = testContext.applicationContext` 重新赋值。

为什么需要：
- `io.kudos.context.kit.SpringKit` 用**静态字段**持有 `ApplicationContext`，正常由
  `SpringContextInitializer`（`ApplicationContextInitializer`）在上下文**创建**时设置。
- Spring Test 框架会**缓存并复用**满足条件的上下文（按 `classes` / `properties` /
  configurations 一致就复用）。复用时 `ApplicationContextInitializer` 不会重跑——
  `SpringKit.applicationContext` 就停留在最初被设置时的那个 context 上。
- 跑多个测试类时，第二个用到 `SpringKit` 的测试就会拿到**已 close 的旧 context** 或
  **另一个测试上下文的 context**，触发 `NoSuchBeanDefinitionException`。

这个 listener 通过 Spring Test SPI（`spring.factories` 里的 `TestExecutionListener` key）
注册，对所有 `@SpringBootTest` 都生效——业务侧不用显式配置。

## `TestApplication`

```kotlin
@EnableKudos
open class TestApplication
```

故意**不**加 `@SpringBootApplication`——避免 Spring Boot 的 auto-configuration 包扫描，
让上下文只装载 kudos 的 `IComponentInitializer` 链（见 [kudos custom auto-config SPI](
../../kudos-context/README.md)）。这样测试上下文足够"最小"，启动快。

业务侧可以通过 `@EnableKudosTest(classes = [MyApp::class])` 替换成自家启动类。

## 已知限制

- ❗ `TestApplication` 是 `open class`，但因为没有 `@SpringBootApplication`，子类化它只
  是为了"标 `@EnableKudos`"。继承层次没有实际用途，业务侧应该走 `classes` 参数而不是
  继承。
- ❗ `SpringKitTestContextListener` 通过 `spring.factories` 注册——Spring Boot 3.x 在某些
  上下文里已迁移到 `META-INF/spring/...imports` 文件。当前形式仍可用，但未来升级
  Spring Test 框架时可能需要迁移。

## 改进建议（自动分析 2026-06-11）

### 测试覆盖
- `test-src/io/kudos/test/common/init/EnableKudosTestTest.kt` 是空壳类（无任何 @Test/断言）。
  建议补一个最小测试：`@EnableKudosTest` 装上下文 + 断言注入的 `ApplicationContext` 与
  `SpringKit.applicationContext` 同一实例——这正是 `SpringKitTestContextListener` 关键修复的
  回归保护，目前完全没有覆盖。

### 可维护性 / 可观测性
- `src/io/kudos/test/common/init/SpringKitTestContextListener.kt`：未覆写 `getOrder()`，与业务侧
  其它 `TestExecutionListener` 共存时执行顺序未定义。建议显式声明 order（晚于
  `DependencyInjectionTestExecutionListener`），并在重新指向 context 时打一条 DEBUG 日志，便于
  排查"SpringKit 指向哪个上下文"类问题。

### 文档
- `init/TestApplication` 的 KDoc 只有一句话，未说明"为什么故意不加 @SpringBootApplication"
  （该设计理由目前只在本 README 里）。建议把理由下沉到 KDoc，避免后人"顺手补注解"。
