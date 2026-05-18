# kudos-ability-data-rdb-ktorm

Ktorm ORM 适配层。在 `kudos-ability-data-rdb-jdbc` 提供的动态数据源能力之上，给业务侧
一套统一的 DAO / Entity / Table 抽象：

1. **CRUD 基类** —— `BaseReadOnlyDao` / `BaseCrudDao`，覆盖按 id / 属性 / Criteria / `SearchPayload`
   的查询、聚合、分页、批量 CRUD
2. **表结构封装** —— `IntIdTable` / `LongIdTable` / `StringIdTable` / `ManagedTable`，统一
   `id` 主键约定及 `IManagedDbEntity` 的审计 / 启用 / 内置 / 备注字段
3. **Database 上下文** —— `KudosContextHolder.currentDatabase()` 按当前请求懒构造 Ktorm
   `Database`；与 Spring `TransactionManager`、Seata 共用同一 `DataSource` bean
4. **表达式工具** —— `SqlWhereExpressionFactory`、`CriteriaConverter`、`XColumnOperation`
   把 kudos `Criteria` / `OperatorEnum` 翻译成 Ktorm 表达式（包含 `ilike` / `ieq` /
   字符串列字典序比较等 Ktorm 原生没有的算子）

## 设计要点

### Database 与 DataSource 同实例约定

`KudosContextHolder.currentDatabase()` 用 `Database.connectWithSpringSupport(...)` 包装
Spring 容器里的 `dataSource` bean——**不再二次套 Seata 代理**。原因（也是历史 bug 教训）：

> 如果在这里 `IDataSourceProxy.proxyDatasource(...)` 再包一层，Spring `TransactionManager`
> 用的 bean 和 Ktorm 用的就是两个不同的 DataSource 实例，`@Transactional` 打开的连接和
> Ktorm 执行 SQL 的连接互不相通——Seata 收不到 `BranchRegister`，Ktorm 写下的数据所在
> 孤儿连接被 Hikari 还池时回滚，业务数据无声地消失。

Seata 兼容请通过 `spring.datasource.dynamic.seata=true` 让 baomidou dynamic-datasource
在 bean 层做代理；详见 `kudos-ability-data-rdb-jdbc` README 的 "Seata 兼容关键"。

### 表 / 实体约定

| 接口 / 抽象类 | 角色 |
|---|---|
| `IDbEntity<ID, E>` | 实体顶层接口，绑定 `IMutableIdEntity<ID>` + Ktorm `Entity<E>` |
| `IManagedDbEntity<ID, E>` | 在前者基础上加 `IActivable` / `IAuditable` / `IHasBuiltIn` / `IHasRemark` |
| `DbEntityFactory<E>` | 替代 `Entity.Factory<E>`，避免业务 PO 直接 import `org.ktorm.*` |
| `IntIdTable<E>` / `LongIdTable<E>` / `StringIdTable<E>` | 单列 `id` 主键 + 类型 |
| `ManagedTable<E>` | `StringIdTable` + 审计字段 + 启用 / 内置 / 备注 |

约定：**每张表恰好一个名为 `id` 的主键列**。`ColumnHelper.columnOf(...)` 在解析失败时会
自动回落到 `table.primaryKeys[0]`，多列主键不被支持。

### 属性名 → 列名解析

`ColumnHelper.columnOf(table, *propertyNames)` 按如下顺序：

1. 驼峰转下划线 → 小写匹配（`userId` → `user_id`）
2. 转大写匹配（H2 / Oracle 默认大写命名）
3. 遍历 `table.columns` 用大小写不敏感对比列名 / 属性名
4. 属性名是 `id` → 回落 `table.primaryKeys[0]`

线程安全：缓存用 `ConcurrentHashMap`；Locale：转大写用 `Locale.ROOT`（避免 Turkish locale
下 `i → İ` 的本地化偏差，旧实现用 `Locale.getDefault()` 在跨主机部署时会偶发解析失败）。

### CriteriaConverter

`Criteria` 内部 "组" 之间是 AND，组内 OR，逻辑由 `Criteria.getCriterionGroups()` 返回的
嵌套结构表达。映射到 Ktorm 表达式时也保持同样的 AND/OR 结构。空 `Criteria` 会触发
`reduce` 失败，调用方需自行避免。

### SearchPayload 与排序白名单

`BaseReadOnlyDao.search(listSearchPayload)` 接受客户端的排序请求，但**只允许 PO 上标了
`@Sortable` 注解的属性参与排序**。未标注的项会打 WARN 并被丢弃。这是一个安全 + 性能护栏：
防止前端构造 `?orderBy=randomColumn` 触发未建索引列上的全表 sort。

未分页时若 `SearchPayload.isUnpagedSearchAllowed()` 返回 false（默认行为），会强制按
`pageNo=1`、`pageSize=getMaxPageSize()` 分页，避免误返回整库数据。

### Seata 兼容（再次强调）

本模块**不要**在任何地方手动代理 DataSource。Database 缓存到 `KudosContext.otherInfos`
后请求结束时一并丢弃。Spring TX、Ktorm、Seata 三者共用 baomidou
`DynamicRoutingDataSource` 在 bean 层装好的（可选）Seata 代理。

## 模块入口

| 路径 | 角色 |
|---|---|
| `datasource/` | `KudosContextHolder.currentDataSource()` / `.currentDatabase()` 扩展函数 |
| `init/KtormAutoConfiguration` | 装配入口（`IComponentInitializer`，按 `@AutoConfigureAfter(JdbcAutoConfiguration::class)` 排在 jdbc 之后） |
| `kit/XRdbKit` | `RdbKit.getDatabase()` 扩展，便于在 jdbc 层 API 旁直接拿 Database |
| `metadata/XColumn` | `Column.getKtormSqlTypeFunName()` 扩展（代码生成用） |
| `support/IDbEntity` / `IManagedDbEntity` | 实体接口 |
| `support/DbEntityFactory` | `Entity.Factory<E>` 别名 |
| `support/IntIdTable` / `LongIdTable` / `StringIdTable` / `ManagedTable` | 表抽象 |
| `support/BaseReadOnlyDao` / `BaseCrudDao` | DAO 基类 |
| `support/ColumnHelper` | 属性名 ↔ Column 解析与缓存 |
| `support/CriteriaConverter` | `Criteria` → Ktorm 表达式 |
| `support/SqlWhereExpressionFactory` | `OperatorEnum` × `value` → Ktorm 表达式 |
| `support/XColumnOperation` | Ktorm 缺失的算子（`ilike` / `ieq` / 列字典序比较） |
| `support/KtormSqlType` | Kotlin 类型 → Ktorm `Schema.<funName>(...)` 列绑定函数名 |

## 配置示例

无独立 yml 配置——所有数据源 / 路由都走 `kudos-ability-data-rdb-jdbc`。本模块只需依赖
传递即可。

## 测试覆盖

- `BaseReadOnlyDaoTest`（831 行）+ `BaseCrudDaoTest`（618 行）：覆盖 DAO 几乎所有公共方法，
  间接覆盖 `ColumnHelper` / `CriteriaConverter` / `SqlWhereExpressionFactory`
- `BaseCrudServiceTest`：DAO + 业务 Service 的集成
- `KtormSqlTypeTest`：纯单元测试，回归类型映射

依赖 testcontainers postgres / H2，需要 Docker 运行环境。

## 已知限制 / 后续工作

- ❗ `KtormSqlType.getFunName()` 的 `Enum` 分支仅在传入 `Enum::class` 本身时命中——具体枚举
  子类（`MyEnum::class`）走不到，会落到 `else -> ""`。代码生成场景目前未触发，但要支持
  自定义枚举到 SQL 类型映射时需要先改这里
- ❗ `Database.connectWithSpringSupport(..., alwaysQuoteIdentifiers = true)` 会把每个标识符
  都加引号——PostgreSQL 下意味着区分大小写，建表时务必全用小写下划线命名（kudos 约定如此），
  否则会出现 `column "MyCol" does not exist` 之类的错
- ❗ `BaseReadOnlyDao.entityClass()` 用 `GenericKit.getSuperClassGenricClass(this::class, 1)`
  解析泛型，只对**直接**继承基类的 DAO 有效；中间隔一层抽象 DAO 时会失败
- ❗ `IntIdTable` / `LongIdTable` / `StringIdTable` 写死 `id` 列名 + 单列主键，多列主键
  / 自定义主键名不支持。需扩展时建议另起一套 `CompositeKeyTable`
- ❗ DAO 没有内置的乐观锁 / 软删除支持；业务侧需要时通过 `updateOnlyWhen` + 自定义
  `Criteria` / `whereConditionFactory` 自己组装
- ❗ `BaseReadOnlyDao` 单文件 1300+ 行，里面 `payload search` / `aggregate` / `processWhere`
  各自独立，可拆成多个 helper object 提升可读性——历史代码，迁移成本不低，暂搁置
- ❗ `getOrderSql` 在 jdbc 模块对单引号做了过滤，本模块的 `sortOf` / `sortBy` 走的是 Ktorm
  `Column.asc()` / `desc()`，属性名由 `ColumnHelper` 解析失败即抛错（不会拼到 SQL 中），
  因此不需要另做注入过滤

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc"))
api(libs.ktorm.core)
api(libs.ktorm.jackson)
testImplementation(libs.h2database.h2)
testImplementation(libs.postgresql)
testImplementation(libs.ktorm.support.postgresql)
testImplementation(project(":kudos-test:kudos-test-common"))
```
