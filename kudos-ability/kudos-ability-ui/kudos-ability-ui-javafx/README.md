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
- ✅ `LinearWizardFlow(Collection?)` 的可空参数现在按空列表处理，不再在构造时 NPE；
  已补 headless 单测覆盖 null / empty flow

## 测试覆盖

- `LinearWizardFlowTest`（2）—— 不启动 JavaFX Stage，覆盖 null / empty flow 的 `canAdvance`
  语义。

2/2 测试全绿。

## 已知限制

- ❗ 控件级测试仍不足 —— TableView / ComboBox / Wizard 页面交互需要 JavaFX toolkit / 可见
  stage，集成测试成本高；建议继续补 `AutoCompleteComboBoxListener` 的 headless 单测
  （Mockito 模拟 ComboBox / TextField）或 TestFX 场景测试
- ❗ `XTableView.terminatingCell` 用 `var` + 延迟初始化（`terminatingCellPropertyImpl()`）——
  非线程安全；JavaFX 单线程模型下不是问题，但加 `@FXThreadOnly` 注解更明确
- ❗ `XTextFieldTableCell` 用 `findTextField()` lookup `.text-field` skin——依赖 JavaFX
  内部 skin 实现细节，未来 javafx 版本升级有破坏风险
- ❗ `XTableCellBehavior` 继承 `com.sun.javafx.scene.control.behavior.TableCellBehavior`
  ——内部 API，需要 `--add-exports` JVM 参数才能在 modular JDK 编译；可能在 javafx 25+
  完全不可用
- ❗ Wizard 组件依赖 controlsfx 风格——可能与业务自定义皮肤冲突
- ❗ 模块跟 kudos 主体 web 服务体系完全独立——可能更适合分离到独立仓库

## 改进建议（自动分析 2026-06-11）

> 本轮已直接修复：`NumberTextFieldTableCellFactory` 整型溢出崩溃（纯数字超过 Int.MAX_VALUE 时
> `toInt()` 抛 NumberFormatException，改 `toIntOrNull()`）；`ValueExtractor.getValue` 双重查表
> 简化 + 纠正"业务可注册"的失实 KDoc；`ImplUtils` if-else 链改 `when`；`XTableView` 惰性属性
> 去掉冗余强转；`XTextFieldTableCell.startEdit` 冗余 `requireNotNull` 链与 `findTextField` 的
> `toTypedArray()[0]`/冗余 `toList()` 清理、删除未用 import；`XTableCellBehavior` 改 `as?` 安全
> 调用；`LinearWizardFlow` 属性就地初始化、去掉 `++index` 写法；`TextFieldCell` 监听器直接引用
> `textField` 字段（不再 `graphic as TextField`）；`Wizard` KDoc 中残留的 `hello.dialog.wizard`
> 失效链接修正。以下为**不宜自动修改**（涉及 public API / 行为变更 / 需要人工决策）的事项。

### 功能缺陷 / 可补充功能
- `wizard/ValueExtractor.kt`：`addValueExtractor` 是 private，业务无法为自定义控件注册取值器
  （对象级 KDoc 曾宣称可注册，本轮已改为如实描述）。建议公开该方法或提供 SPI —— 属 public API
  扩充，留人工决策。
- `wizard/ValueExtractor.kt`：按 `n.javaClass` **精确匹配**，控件子类（如业务继承 `TextField`
  的输入框）不会命中任何取值器。建议改为沿类层级 `isInstance` 查找（行为变更）。
- `wizard/Wizard.kt`：`readSetting` 对无 id 节点生成 `page_.setting_N`，而 `settingCounter`
  在**每页**`readSettings` 时归零 —— 多页间同序号 key 互相覆盖；多页同 id 节点亦覆盖。建议 key
  里带页索引（行为变更，影响既有调用方取值）。
- `table/cell/factory/NumberTextFieldTableCellFactory.kt`：`isNumeric` 仅接受 Unicode 数字字符，
  负数 "-5" 会被静默清为 null。若业务需要负整数，应整体改为 `string.trim().toIntOrNull()`
  （行为变更）。
- `wizard/Wizard.kt`：缺 `initOwner` API（源码 TODO）、缺 Finish 前的整体校验回调
  （ValidationSupport 一段被注释掉）。

### 安全性
- `wizard/ValueExtractor.kt` + `wizard/Wizard.kt`：`PasswordField.text` 被**明文**收进
  `Wizard.settings`（ObservableMap，长期驻留、可能被日志/序列化扩散）。建议对密码框跳过收集
  或用后即清，由调用方显式取值。

### 测试覆盖
- `test-src` 仅 `LinearWizardFlowTest` 2 个用例。`LinearWizardFlow.advance` 的越界语义、
  `ValueExtractor` 各控件取值、`AutoCompleteComboBoxListener` 过滤/光标逻辑、
  `XTableView.terminateEdit` 状态机均无覆盖。建议引入 TestFX + Monocle（headless glass）跑
  控件级测试；`ValueExtractor` 大部分控件可在初始化 toolkit 后直接 new 出来断言。

### 可扩展性
- `controls/XTableCellBehavior.kt`：继承 `com.sun.javafx...TableCellBehavior` 内部 API，
  modular JDK 需 `--add-exports`，JavaFX 后续版本可能直接编译失败。建议提供基于公开 API
  （监听 `focusModel.focusedCellProperty` 触发 `terminateEdit`）的替代实现并逐步迁移。
- `controls/TextFieldCell.kt`：focus/hover 样式为硬编码内联 CSS 字符串（purple 主题魔法值）。
  建议抽到样式表，用 `:focused`/`:hover` 伪类实现，便于业务换肤。
- `controls/XTextFieldTableCell.kt`：`findTextField` 依赖 `.text-field` skin lookup 与
  graphic 结构假设，属 JavaFX 实现细节，版本升级有破坏风险（既有已知限制，维持跟踪）。

### 可观测性
- 全模块 0 行日志。至少两处静默吞错值得补 debug 日志（kudos-base 已有日志门面）：
  `XTextFieldTableCell.startEdit` 查不到 TextField 时静默 return；`XTableCellBehavior`
  宿主非 XTableView 时静默跳过 terminate。

### 可维护性
- `controls/XTextFieldTableCell.kt`：`terminatingListener` 为**强引用** ChangeListener 挂在
  XTableView 属性上（源码自注 "TBD: cleanup, probably needs WeakListener"），cell 被丢弃后
  无法 GC，长生命周期表格存在内存滞留。建议改 `WeakChangeListener`（行为敏感，需测试配合）。
- `controls/TextFieldCell.kt`：`updateItem` 在 `sc == null` 时执行 `item as String`，列类型
  非 String 会 ClassCastException；建议回退 `item.toString()`（行为变更）。同文件
  `boundToCurrently` 死字段与大段注释代码建议清除。
- `wizard/Wizard.kt`：`BUTTON_PREVIOUS_ACTION_HANDLER` 等**实例** val 用 SCREAMING_SNAKE
  常量命名风格，建议 camelCase（重命名，未自动改）；`updatePage` 一函数承担读 settings/装按钮/
  换页三职责，可拆分。
- `wizard/Wizard.kt`：`dialog` 为 `var Dialog? = null` 但 init 必赋值，可改非空 val 并删除
  `requireDialog`（牵动多处私有逻辑，建议随上一条一起重构）。

### 对外 API 稳定性
- `wizard/Wizard.kt`：`getProperties()` 返回可空但实现永不为 null；`Wizard()` 次构造与
  `title: String = ""` 默认参数重复（Java 互操作宜改 `@JvmOverloads` 并删次构造）；
  `showAndWait(): Optional<ButtonType?>` 双重可空语义冗余。均属二进制兼容敏感项，留大版本。
- `controls/AutoCompleteComboBoxListener.kt`：类型参数 `T` 与构造参数 `ComboBox<Any>` 脱节；
  `handle` 用过滤副本**整体替换** `comboBox.items`，外部持有的原 items 引用与控件脱钩。建议
  泛型统一为 `ComboBox<T>` 并改用 `FilteredList`（破坏 API）。
- `controls/XTableView.kt`：泛型暴露为 `TablePosition<S?, *>?`（S? 而非 S），调用方书写别扭，
  属可斟酌的 API 形态问题。

### 文档
- 组级 README 与本 README 缺 Wizard 完整使用示例（含自定义 `Flow` 分支流程、`settings` 取值
  约定 —— 尤其是上文"无 id 节点跨页覆盖"陷阱）；`ValueExtractor` 支持的控件清单建议表格化。
