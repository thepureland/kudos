# kudos-test-common

最基础的测试支持：`@EnableKudosTest` 注解 + Spring 测试上下文。

## 内容

| 文件 | 角色 |
|---|---|
| `init/EnableKudosTest` | meta-注解：组合 `@SpringBootTest` + `@ContextConfiguration(classes = [TestApplication::class])` + kudos 组件初始化器扫描 |
| `init/TestApplication` | 测试用的 `@SpringBootApplication` —— 让 Spring 启动一个最小化的 kudos 上下文 |
| `init/SpringKitTestContextListener` | 让 `io.kudos.context.kit.SpringKit` 在测试期也能拿到 ApplicationContext |

## 使用

```kotlin
@EnableKudosTest
class MyTest {
    @Resource lateinit var myService: MyService
}
```

`@EnableKudosTest(properties = ["foo=bar", "..."])` 透传额外 Spring 属性。
