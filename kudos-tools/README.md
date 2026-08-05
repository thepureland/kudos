# kudos-tools

开发期工具集合——**不参与运行时**，只在开发 / 部署阶段使用。

## 内容

### 代码生成器（`io.kudos.tools.codegen`）

基于 Freemarker 模板的代码生成器：从 DB 元数据生成 `core` / `client` / `api-*` / `common`
等标准 7 子模块的骨架代码。配合 JavaFX UI 操作。

| 包 | 角色 |
|---|---|
| `codegen.core` | Freemarker 模板加载 + 模型构建 + 输出 |
| `codegen.core.merge` | 增量合并：保留用户改过的代码段 + 替换框架生成段 |

模板目录：`resources/templates/kudos/${project}-ams-${module}-...`

### SQL 工具

`resources/sql/codegen/` 下的脚本——给代码生成器提供"按表生成模板"的支持。

### UI

`resources/fxml/` —— JavaFX 界面，配合 `kudos-ability-ui-javafx` 模块运行。

## 启动

```bash
./gradlew :kudos-tools:run
```

或 IDE 内直接跑 main 类。

## 已知限制

- ❗ 代码生成器和 JavaFX UI 强绑定——纯命令行 / CI 化使用需要重新封装
- ❗ 模板里的路径占位符（`${project}-ams-${module}-*`）反映了**早期**项目结构；如果未来
  调整 ms 子模块约定，模板需要同步更新
- ❗ 合并器（`PrivateContentEraser` / `UserCodesRetriever`）依赖代码里的特定标记注释；
  违反约定的代码可能在重新生成时丢失

## 改进建议（自动分析 2026-06-11）

### 1. Kotlin 写法

- `src/io/kudos/tools/codegen/model/vo/Config.kt`、`ColumnInfo.kt`、`DbTable.kt`、`GenFile.kt`：
  手写 JavaFX 属性三件套（`getX`/`xProperty`/`setX`）样板代码量大，可考虑抽公共基类或用属性委托收敛
  （因 FXML 反射依赖方法名，未直接改动）。
- `src/io/kudos/tools/codegen/core/TemplateModelCreator.kt`：`initOtherParameters` 中六段
  "按列分组判断是否含某 Kotlin 类型"的循环几乎完全相同，可抽成
  `fun fillTypeFlags(suffix: String, columns: List<Column>)` 一类的辅助函数消重。
- `src/io/kudos/tools/codegen/dao/CodeGenColumnDao.kt`：`searchCodeGenColumnMap` 的手写
  HashMap 循环可用 ktorm 的 `associateBy { it.name }` 一行替代。

### 2. 功能缺陷与补充点

- `src/io/kudos/tools/codegen/fx/controller/ConfigController.kt`（`initTempleComboBox`）：模板目录
  写死为 `${PathKit.getRuntimePath()}/../../../resources/main/templates/`，强依赖 Gradle 输出布局，
  打成 jar 或换构建目录后即失效；建议同时支持 classpath 扫描与用户自定义模板目录。
- `src/io/kudos/tools/codegen/fx/controller/ConfigController.kt`（`initConfig`）：模板名读取的是
  `PROP_KEY_TEMPLATE_ROOT_DIR`，而 `storeConfig` 已写入 `PROP_KEY_TEMPLATE_NAME`，重启后模板下拉
  显示的是路径而非名称；建议优先读 TEMPLATE_NAME 并以 ROOT_DIR 兜底。
- `src/io/kudos/tools/codegen/core/merge/`（`CodeMerger`/`UserCodesRetriever`/`AppendCodesRetriever`/
  `PrivateContentEraser`）：region 编号正则均为单个 `\d`，模板内 region 编号 ≥ 10 时会静默失配丢内容；
  建议统一改为 `\d+`（涉及行为面较广，未直接改）。
- `src/io/kudos/tools/codegen/fx/controller/BatchGenerationController.kt`（`generate`）：批量生成失败
  只弹 "Generation failed!"，不指明哪张表、哪个文件失败，建议在 Alert 与日志中携带表名/文件名。
- `src/io/kudos/tools/sql/EmptySqlFileCreator.kt`：文件头 TODO（如何独立打包）未解决；版本号解析
  `file.name.substring(0, 5)` 为魔法值，文件名不满足约定时会越界。
- 整体：代码生成与 JavaFX 强绑定，无 CLI/headless 入口（已知限制中已提）；建议抽出无 UI 的
  `CodeGenerationFacade(config, tables) -> List<File>` 供 CI / 脚本调用。

### 3. 安全性

- `~/.kudos/CodeGenerator.properties` 中数据库密码明文持久化（`ConfigController.storeConfig`）；
  虽为开发期工具（Config KDoc 已注明），仍建议至少提供"不保存密码"选项或接入 OS 凭据库。
- `src/io/kudos/tools/sql/BatchSqlExecutor.kt`、`EmptySqlFileCreator.kt`：硬编码本机 JDBC 连接串与
  个人桌面路径，应改为从 `main(args)`/环境变量读取，避免误连他人环境。
- 路径穿越（理论风险）：`TemplatePathProcessor.readPaths` 把 Freemarker 渲染后的模板相对路径直接拼到
  输出根目录（`CodeGenerator.executeGenerate`），若引入第三方模板包，含 `..` 的路径可写出输出目录之外；
  建议生成前对 normalize 后的目标路径做"仍在输出根目录内"校验。

### 4. 测试覆盖

- 本次已补充 `CodeMergerTest`、`PrivateContentEraserTest`、`FreemarkerKitTest`（见 test-src）。
- 仍缺测试：`TemplatePathProcessor`、`TemplateModelCreator`、`CodeGen*Service`、`CodeGen*Dao` ——
  它们依赖 `CodeGeneratorContext` 全局单例与真实数据库连接，需先解耦（见第 5 点）才可单测。

### 5. 可扩展性

- `src/io/kudos/tools/codegen/core/CodeGeneratorContext.kt`：全局可变 `lateinit` 单例承载所有状态
  （tableName/columns/config），隐式跨页面传递，无法并发、难以复用与单测；建议改为显式的上下文对象
  随调用链传参，UI 层只保留一份引用。
- `TemplateModelCreator.create()`/`createBaseModel()` 非 `open`，子类只能定制
  `determinePoDaoSuperClass`/`initOtherParameters`，无法增删基础模型键；建议开放或提供 hook。
- 元数据读取直接调用 `RdbMetadataKit` 静态方法，数据库方言、表过滤规则（`CodeGenObjectService`
  中写死的 `flyway_` 前缀与 code_gen_* 排除表）均不可配置；可定义元数据提供者接口。

### 6. 可观测性

- 各 FX 控制器与向导（`ConfigController.storeConfig`、`BatchGenerationController.generate`、
  `FilesController.generate`、两个 Wizard 的 `println`）大量使用 `e.printStackTrace()`/`println`
  而非日志框架，建议统一换成 `LogFactory`。
- 生成过程缺少 info 级进度日志（当前仅本次新增的持久化失败 warn 与 TemplateReader 的 debug），
  建议每个文件生成/合并时输出一条 info。

### 7. 可维护性

- `Config.codeLoaction` 拼写错误已固化为方法名与配置键（KDoc 已注明历史原因）；建议新增正确拼写的
  键并兼容读取旧键，逐步废弃。
- `src/io/kudos/tools/codegen/fx/ui/SortComboBoxTableCellFactory.kt`：未被任何 Kotlin 代码或 FXML
  引用，疑似死代码，确认后可删除（删除属 public API 变更，本次未动）。
- `ConfigController.canGoOn`：六段顺序校验抛裸 `Exception`，建议拆成"校验器列表 + 专用异常类型"。
- `TemplatePathProcessor.readPaths`：`templateRootDir.lastIndex + 2`、`directory.replace('.', '/')`
  属魔法逻辑（目录名中的点会被意外转成路径分隔符），建议封装命名函数并加注释/测试。
- `TemplateReader` 每次 `read` 新建 Freemarker `Configuration`，且 `TemplatePathProcessor` 与
  `FilesController.selectEntityRelativeFiles` 为判断 entity 相关性重复读取整份模板，模板多时有明显
  性能浪费；可在一次向导会话内缓存。

### 8. 对外接口（public API）

- `CodeGen*Service`/`CodeGen*Dao`/`CodeGeneratorContext` 均为 public object 单例，外部可直接改写
  内部状态；建议收敛为 `internal`（属 API 变更，未直接改）。
- `FreemarkerKit.getAvailableAutoInclude` 声明返回 `List<String?>` 但实际永不含 null，宜改为
  `List<String>`（签名变更，未直接改）。
- `MultiTablesCodeGenerateWizard`/`SingleTableCodeGenerateWizard.getTemplateModelCreator()` 是清晰的
  扩展点，保持稳定即可。

### 9. 文档

- 建议在 README 或模板目录下补充：模板可用占位符清单（`project`/`module`/`packagePrefix`/
  `entityName`/`columns`/`pkColumn`/`contains*Column*` 等，见 `TemplateModelCreator`）以及
  `//region your codes N` / `//region append <TYPE> codes N` 合并标记的书写约定，方便模板作者。
