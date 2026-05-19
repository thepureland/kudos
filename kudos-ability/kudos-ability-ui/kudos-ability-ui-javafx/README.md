# kudos-ability-ui-javafx

JavaFX UI 控件库——给桌面 / hybrid 应用提供 kudos 增强的 TableView / ComboBox / Wizard
等组件。**与 kudos-ms / kudos-ability 的其余 web 体系完全独立**——纯 GUI 模块。

## 模块入口

| 路径 | 角色 |
|---|---|
| `controls/XTableView` | 扩展 TableView，支持 `terminateEdit()` —— 切换行 / 失焦时主动 commit 编辑 |
| `controls/XTableCellBehavior` | 配套的 cell 行为，simpleSelect 前调用 `terminateEdit` |
| `controls/TextFieldCell` / `XTextFieldTableCell` | 增强的可编辑文本 cell |
| `controls/table/cell/factory/*` | 三个 CellFactory（CheckBox / NumberTextField / XTextField） |
| `controls/AutoCompleteComboBoxListener` | 给 ComboBox 加输入即时过滤的 KeyEvent handler |
| `controls/wizard/Wizard` + `LinearWizardFlow` + `ImplUtils` + `ValueExtractor` | 多步向导组件 |

## XTableView 的"主动 commit"模式

JavaFX 原生 TableView 在切换行时不会自动 commit 当前编辑 cell 的值——业务侧经常踩坑。

`XTableView.terminateEdit()` 提供主动 commit API：
- 配套 `XTableCellBehavior` 在 `simpleSelect`（点击其他行 / 列）之前调一下
- 配套 `XTextFieldTableCell` 监听 `terminatingCellProperty()` 在通知时 commit

**约定**：在 `XTableView` 中所有可编辑 cell 都必须用扩展版 `XTableCellBehavior`，否则
切行不会触发 commit。

## AutoCompleteComboBox

```kotlin
val combo = ComboBox<Any>()
AutoCompleteComboBoxListener<String>(combo)  // 注册即生效
```

按用户输入实时过滤 items；BACK_SPACE / DELETE 时按当前 caret 位置过滤，方向键 + Ctrl 跳过
过滤逻辑。

## 配置 / 构建

```kotlin
plugins {
    alias(libs.plugins.javafx)
}

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    api(project(":kudos-base"))
    api(libs.controlsfx)
}
```

依赖：JavaFX 自身 + controlsfx。**JVM 启动时需要 JavaFX 模块**——非 Java 21+ 内置场景需
按 OS 平台分发 JavaFX runtime。

## 已修复（本轮 8 维度审计）

- ✅ `AutoCompleteComboBoxListener` 参数从 `ComboBox<Any>?` 改为非空 `ComboBox<Any>`，
  删除方法体内 8 处 `requireNotNull(comboBox)`——构造时已保证非空，方法体里再
  nullable 是冗余。`when` 重写键码分支替换 `if-else if` 链
- ✅ `AutoCompleteComboBoxListener` 显式标注 `.lowercase()` 走 Locale.ROOT（Kotlin
  1.5+ 默认）—— 让"为什么不写 Locale 参数"的疑问有 inline 答案
- ✅ `XTextFieldTableCell.startEdit` 内的 `System.out.println(tf.text)` + `println("commit")`
  调试输出移除
- ✅ `XTableCellBehavior` 未使用的 `private val LOG = Logger.getLogger(...)` 字段移除
  + 删除 `import java.util.logging.Logger`

## 已知限制

- ❗ **没有测试** —— UI 组件需要可见 stage 才能跑，集成测试成本高；该模块靠手工验证。
  建议至少补 `AutoCompleteComboBoxListener` 的 headless 单测（Mockito 模拟 ComboBox /
  TextField）
- ❗ `XTableView.terminatingCell` 用 `var` + 延迟初始化（`terminatingCellPropertyImpl()`）——
  非线程安全；JavaFX 单线程模型下不是问题，但加 `@FXThreadOnly` 注解更明确
- ❗ `XTextFieldTableCell` 用 `findTextField()` lookup `.text-field` skin——依赖 JavaFX
  内部 skin 实现细节，未来 javafx 版本升级有破坏风险
- ❗ `XTableCellBehavior` 继承 `com.sun.javafx.scene.control.behavior.TableCellBehavior`
  ——内部 API，需要 `--add-exports` JVM 参数才能在 modular JDK 编译；可能在 javafx 25+
  完全不可用
- ❗ Wizard 组件依赖 controlsfx 风格——可能与业务自定义皮肤冲突
- ❗ 模块跟 kudos 主体 web 服务体系完全独立——可能更适合分离到独立仓库
