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
