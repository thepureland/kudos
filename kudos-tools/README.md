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

模板目录：`resources/templates/kudos/${project}-ms-${module}/${project}-ms-${module}-<子模块>/...`，
与 `kudos-ms/kudos-ms-sys` 的 7 子模块布局一一对应（见 [kudos-ms/README.md](../kudos-ms/README.md)）。

#### 生成的文件清单

| 子模块 | 与实体无关（每模块一份） | 与实体相关（每张表一份） |
|---|---|---|
| `*-common` | `build.gradle.kts` | `<e>/api/I<E>Api.kt`、`<e>/enums/<E>ErrorCodeEnum.kt`、`<e>/vo/<E>CacheEntry.kt`、`<e>/vo/request/{I<E>FormBase,<E>FormCreate,<E>FormUpdate,<E>Query}.kt`、`<e>/vo/response/{<E>Detail,<E>Edit,<E>Row}.kt` |
| `*-sql` | `build.gradle.kts` | —（Flyway 脚本仍需手写） |
| `*-core` | `build.gradle.kts`、`core/platform/init/<Module>AutoConfiguration.kt`、`test-resources/application.yml` | `<e>/api/<E>Api.kt`、`<e>/dao/<E>Dao.kt`、`<e>/event/<E>Events.kt`、`<e>/model/po/<E>.kt`、`<e>/model/table/<E>s.kt`、`<e>/service/impl/<E>Service.kt`、`<e>/service/iservice/I<E>Service.kt` |
| `*-client` | `build.gradle.kts` | `<e>/proxy/I<E>Proxy.kt`、`<e>/fallback/<E>Fallback.kt` |
| `*-api-admin` | `build.gradle.kts`、`init/` 两个类、`resources/` 与 `test-resources/` 的 `application.yml` | `controller/<e>/<E>AdminController.kt` |
| `*-api-internal` | `build.gradle.kts`、`init/` 两个类 | `controller/<e>/<E>InternalController.kt` |
| `*-api-public` | `build.gradle.kts`、`init/` 两个类 | —（按约定不挂业务控制器） |

`<E>` = `entityName`（如 `SysParam`），`<e>` = `bizModule`（**业务模块**，如 `param`）。
含 `${entityName}` 的模板会被 `TemplatePathProcessor` 判定为"与实体相关"，只在单表生成时产出；
其余模板只能引用**基础模型**里的占位符。

#### 模板可用占位符

由 `TemplateModelCreator` 注入，分两批：

| 占位符 | 批次 | 含义 |
|---|---|---|
| `project` / `packagePrefix` / `module` / `moduleCapitalize` / `author` / `version` | 基础 | 模板名、包前缀、原子服务名、其首字母大写形式、`@author` / `@since` |
| `entityName` / `shortEntityName` / `lowerShortEntityName` | 实体 | `SysParam` / `Param` / `param` |
| `bizModule` | 实体 | **业务模块**，即 `common/` `core/` `client/` `api-*/controller/` 下的一级目录。见下方「业务模块」小节 |
| `table` / `columns` / `pkColumn` / `ktormFunNameMap` | 实体 | 表元数据；`columns` 已剔除父类（`ManagedTable` / `*IdTable`）自带的列 |
| `searchItemColumns` / `listItemColumns` / `editItemColumns` / `detailItemColumns` / `cacheItemColumns` | 实体 | 向导里勾选的五组列 |
| `poSuperClass` / `daoSuperClass` | 实体 | `IManagedDbEntity`\|`IDbEntity` / `ManagedTable`\|`StringIdTable`\|`IntIdTable`\|`LongIdTable`\|`Table` |
| `contains<Type>Column` 及其 `In{Search,List,Edit,Detail,Cache}Items` 变体 | 实体 | 驱动条件 import |
| `serialVersionUID` | 实体 | 随机值。**当前模板不用它**——`<E>CacheEntry` 固定写 `1L`，理由见模板内注释（该行在 region 外，随机值会导致每次重新生成都变、击穿 Redis 里已有的缓存条目）。保留此键是为了不破坏自定义模板 |

`macro.include` 会被自动 include，提供 `generateClassComment`、`responseVoProperty`、
`formVoProperty`、`formVoOverrideProperty` 四个宏与 `DEFAULT_LITERALS` 变量。

#### 业务模块（`bizModule`）

`common` / `core` / `client` / `api-admin,api-internal 的 controller` 四处的**一级目录**，
按业务模块分组而不是一表一目录 —— 一个业务模块通常聚合多张表：

| 业务模块 | 包含的表 |
|---|---|
| `dict` | `sys_dict`、`sys_dict_item` |
| `accessrule` | `sys_access_rule`、`sys_access_rule_ip` |
| `account` | `user_account`、`user_account_third`、`user_account_protection`、`user_org_user` |
| `login` | `user_login_remember_me`、`user_log_login` |

这个分组**无法从表名推导**（`user_org_user` 归在 `account` 而不是 `org`），所以由向导输入：

- 单表向导：第 2 页「业务模块」输入框
- 批量向导：表格里可编辑的「业务模块」列

默认值取 `TemplateModelCreator.defaultBizModule()`（＝实体短名小写），对每个业务模块的**主表**是对的
（`sys_param` → `param`），**从表必须手工改**（`sys_dict_item` 默认给出 `dictitem`，应改成 `dict`）。
填过的值持久化在 `code_gen_object.biz_module`，下次自动回填；留空则回退到默认值。

> URL 段用的是 `${shortEntityName?uncap_first}`（驼峰，如 `/api/admin/sys/dictItem`），
> 与 `bizModule` 是两个概念，不要混用。

#### 跨模块约定取舍（2026-08-18 定）

`kudos-ms` 四个原子服务在若干写法上不一致，模板必须选一边。已定的部分：

| 约定 | 模板取值 | 理由 |
|---|---|---|
| Events 文件名 | `<E>Events.kt`（全实体名） | user / auth 用全名，只有 sys 用短名且自身已破例（`SysCacheEvents` vs `SystemEvents`）；里面的 `sealed interface` 本来就是全名 |
| `<E>Row` | 实现 `IIdEntity<PK>` | user / auth / msg 都实现；sys 的 `Detail`/`Edit` 也都实现，只有 `Row` 不实现，属 sys 内部不一致 |
| `serialVersionUID` | 固定 `1L` | 该行在 region 外，随机值会让每次重新生成都变，击穿 Redis 里已有的缓存条目 |
| `i18nKeyPrefix` | `<module>.error-msg.<bizModule>` | sys 是四家里唯一自洽的（前缀 ↔ 业务模块一一对应） |
| Fallback 基类 | `AbstractFeignFallbackSupport` | 四个 ms 模块现已全部直接继承；sys 原有的 `SysClientFallbackSupport` 别名已删除 |
| `*-common` 的 `KotlinCompile` 块 | 不生成 | 全仓库只有 sys-common 有；`javaParameters` 对 `jackson-module-kotlin` 无必要，`-Xjvm-default=all` 在 Kotlin 2.2+ 已是默认且该 flag 已废弃 |
| init 类名 | `<Module>ApiInternalApplication` / `<Module>ApiPublicApplication` | gradle 模块名（`-api-internal` / `-api-public`）和 `getComponentName()` 早已是 internal/public，只有类名停在 `Provider` / `Web`；四个 ms 模块已同步改名 |

未定（模板暂时保持现状）：

- **Feign 服务名**：sys 全小写拼接 vs 其余三家 kebab。但 `kudos-ms` 下**没有任何 `spring.application.name`**，
  而部署单元是 `<module>-api-internal`——这批"每实体一个服务名"很可能压根注册不上，属设计问题而非风格问题，
  需先确认线上注册名再决定

#### 合并标记约定

- `//region your codes N` … `//endregion your codes N`：用户代码区，重新生成时**保留旧文件内容**。
- `//region append <PARTIBLE|IMPARTIBLE> codes N` … `//endregion ...`：模板追加区，首次生成时被
  `PrivateContentEraser` 抹掉，重新生成时追加进对应编号的用户代码区。
- ⚠ 编号正则是单个 `\d`，**只能用 0–9**；yml 等 `#` 注释语言写成 `#//region your codes N`。
- 同一个文件的不同分支（如"零查询列 / 有查询列"）必须保持**相同的 region 编号与结构**，
  否则改动列选择后重新生成会把旧区块拼进新骨架里。

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
- ❗ 模板以 `kudos-ms-sys` 为基准（2026-08-18 对齐）；如果未来调整 ms 子模块约定，模板需要同步更新
- ❗ 生成的是**骨架**：`*-core` 的多级缓存（`<e>/cache/`）、`*-sql` 的 Flyway 脚本、
  以及 `I<E>Api` 上除 `get(id)` 之外的领域方法都需要手写；`I<E>Api` 新增方法后，
  `core` 的 `<E>Api`、`api-internal` 的 `<E>InternalController`、`client` 的 `<E>Fallback` 三处要同步补齐
  （前两者编译期有约束，fallback 没有）
- ❗ 视图（`VIEW`）走只读链路：`BaseReadOnlyDao` / `BaseReadOnlyService` / `BaseReadOnlyController`，
  此时生成的 `<E>FormCreate` / `<E>FormUpdate` / `<E>Edit` 用不上，可在向导的文件列表里取消勾选
- ❗ 合并器（`PrivateContentEraser` / `UserCodesRetriever`）依赖代码里的特定标记注释；
  违反约定的代码可能在重新生成时丢失
- ❗❗ **不要用生成器覆盖现存的 `kudos-ms/`**：`CodeMerger` 只保留 `//region your codes N` 区块内的内容，
  而 `kudos-ms` 下 1600+ 个源文件**一个标记都没有**，重新生成等于整文件替换、手写逻辑全丢。
  对已有模块只能人工比对后定点改

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
