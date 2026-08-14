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

缓存**按 Table 实例**而非表名分桶。Ktorm 允许同一张物理表存在多个 Table 对象（窄投影、自连接
用的 `aliased()`），而 `Column` 归属于注册它的那个 Table 对象；按表名分桶会让它们共用一个缓存
条目、把属于另一个 Table 对象的 Column 交出去，渲染进 SQL 就指向了错误的表。`BaseTable` 的
`equals` / `hashCode` 是 final 的恒等实现，所以这本身就是一个 identity cache。代价是：不要在
每次查询时新建 Table 对象（如逐次 `aliased()`）再走这里，按 Ktorm 的意图把它放进字段或
`ShardedTableFactory` 里持有。

### CriteriaConverter

`Criteria` 内部 "组" 之间是 AND，组内 OR，逻辑由 `Criteria.getCriterionGroups()` 返回的
嵌套结构表达。映射到 Ktorm 表达式时也保持同样的 AND/OR 结构。

空 `Criteria` 会被 `require` 拦下并抛出带表名的说明，而不是让 `reduce` 抛一个什么都不说的
`UnsupportedOperationException`。这个报错最常见的来源是 **`Criteria` 自身在构造期丢条件**：
`Criteria` 只保留值非 null / 非空的 criterion（`IS_NULL` / `IS_NOT_NULL` / `IS_EMPTY` /
`IS_NOT_EMPTY` 这类 `acceptNull` 的算子除外），所以
`Criteria.of("status", EQ, null)` 得到的是**没有任何条件**的 Criteria。此时宁可报错，也不能
把调用方的过滤条件悄悄丢掉去跑一个全表查询。

### 行级数据权限的覆盖面

**每一个自行拼 `where` 的查询都必须经过 `BaseReadOnlyDao.scopedWhere(...)`**（`Query` 用
`whereScoped(...)`，`EntitySequence` 用 `readSequence()`），写操作用 `withRowScope(...)`。
这不是风格问题：早期版本只有 `readSequence()` 这一条路加了行过滤，`searchByPayload` /
投影查询 / `get(id, returnType)` / `update(entity)` / `updateProperties` 各自拼 where，
于是全部**未过滤**——从调用点看，覆盖了大部分方法的过滤器和覆盖了全部方法的过滤器长得一模一样。

`RowScopeDaoCoverageTest` 是这条约定的执行者：每个公开入口一条断言。新增 DAO 方法时请
一并在那里加一行。

注意 Ktorm 的 `EntitySequence.update(entity)` / `add(entity)` **不认序列上的 filter**
（传入被 filter 过的序列会直接抛 `UnsupportedOperationException`），所以写操作不能靠
`readSequence()` 保护，只能在各自的 where 上显式 AND。

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

### 聚合函数在空结果集上的语义

- `sum(...)` 返回 `0`（空集之和就是 0，非空返回类型也只能这么表达）
- `avg(...)` **抛 `IllegalStateException`**：空集的平均值没有定义，返回 0 会让一个错误的数字
  悄悄进入业务计算
- 需要区分"没有匹配行"和"结果确实是 0"时用 `sumOrNull(...)` / `avgOrNull(...)`

（旧实现是 `as Number`，SQL NULL 会以一个从框架内部抛出的裸 NPE 呈现。）

### 返回类型：别再用 `List<*>`

`search(listSearchPayload)` 的元素类型由 payload 里 `returnProperties` 的**数量**决定：0 个返回
实体、恰好 1 个返回裸值、2 个及以上返回 `Map`。这条规则不存在于类型系统里，后果是每个调用点都要
强转，而且把 payload 从 2 个返回属性减到 1 个时，元素类型会在"仍然编译通过"的调用点下面悄悄改变。
它还一路传染：`pagingSearch(payload)` 之所以返回 `PagingSearchResult<*>`，就是因为
`search(payload)` 说不出自己返回什么。

旧方法保留（改签名会波及每一个 ms-* service），新代码请用说得清形状的这几个——DAO 与 Service
两层都有：

| 场景 | 方法 | 返回 |
|---|---|---|
| 整个对象（实体 / VO） | `search(payload, returnItemClass)` | `List<T>` |
| 多列投影 | `searchMaps(payload)` | `List<Map<String, Any?>>` |
| 单列取值 | `searchValues(payload)` | `List<Any?>` |
| 分页 + 总数（Criteria） | `pagingSearchWithTotal(criteria, pageNo, pageSize, ...)` | `PagingSearchResult<E>` |
| 分页 + 总数（payload） | `pagingSearchWithTotal(payload, returnItemClass)` | `PagingSearchResult<T>` |

注意 `searchMaps` **不管几个返回属性都返回 Map**（1 个时也是），这正是它与 `search` 的区别；
`searchValues` 则要求恰好 1 个，否则明确报错而不是换一种形状糊弄过去。

`searchMaps` / `searchValues` / `pagingSearchWithTotal(payload, ...)` 都各有一个带
`whereConditionFactory` 的重载（与 `search` 一致，只存在于 ktorm 层，因为参数类型是 Ktorm 的
`Column` / `ColumnDeclaring`）。否则"既要自定义条件、又要明确返回类型"的查询只能退回
`search(...)` 强转，正是这几个方法要消灭的东西。

### 分页与总数要一次拿

`pagingSearch` + `count` 是两个必须彼此保持条件一致的调用点，而用不同条件算出来的总数就是一个
会说谎的分页器。`pagingSearchWithTotal(...)` 一次给出当前页和总数：Criteria 版两条语句共用同一个
`criteria` 对象，payload 版只构建一次查询，有 `LIMIT` 时用 Ktorm 的 `totalRecordsInAllPages`
（它从同一棵表达式树生成 count SQL），无 `LIMIT` 时总数就是返回行数、根本不需要第二条语句。

（实现上要先解码行、再取总数：无 `LIMIT` 时 Ktorm 通过物化 row set 来回答总数，而它的迭代器不
回绕。）

### 写入值校验：过滤管不到的那一半

行过滤回答的是"我能碰**哪些已存在的行**"，落成 WHERE 子句。有两件事它天生表达不了：

- `insert` 没有已存在的行可过滤。受限于 `org-a` 的用户可以建一条 `org_id = 'org-b'` 的行——
  自己读不到、改不了、删不掉，但对 org-b 是**完全真实的一行**。
- `update` 的 **SET 值不在 WHERE 的管辖内**。`updateProperties(id, mapOf("orgId" to "org-b"))`
  作用在一条自己看得见的行上：从自己这边看等同于删除，从 org-b 那边看是凭空多出一条。

两者归结为同一条规则：**不能写入会把这行推出你自己范围的值**。这是对**值**的判断，在 SQL
构造前于内存中完成，所以由 `RowScopeWriteValidator` 负责，而不是谓词构建器。

未设置的值怎么处理，决定了这套东西会被采纳还是被绕开：

| 待写入的值 | insert | update |
|---|---|---|
| 已设置且在授权范围内 | 通过 | 通过 |
| 已设置但超出范围 | 拒绝 | 拒绝 |
| 未设置 / null，且主体恰好一个可选值 | **自动填充** | 不涉及（该列保持原值） |
| 未设置 / null，且主体多个可选值 | 拒绝，要求显式指定 | 不涉及 |
| 把 scope 列置为 null | 拒绝 | 拒绝（等于对所有人隐藏该行） |

`@DataScopedSelf` 声明的创建者列在 insert 时**强制写成当前主体**：一行是谁创建的是请求的事实，
不是请求的输入项。update 时改写该列同样被拒——转移归属请走 `runAsSystem`。

开关与读侧**完全共用**（`enabled` / `shadowMode` / `runAsSystem` / `unrestricted` / 未声明实体），
判定逻辑写在 `RowScopeEnforcer` 里而不是另起一个组件，就是为了防止两侧漂移。shadow 模式下违规
只记录不拒绝，记为 `WOULD_REJECT`——**上线前请先读这一类**，它对应的是失败的请求，而不是变短的
结果集。

拒绝时抛 `RowScopeViolationException`。它区别于 `RowScopeUnresolvedException`（"没人在问"）：
这里是有人在问、答案是不行。应用层通常想把它映射成 403 而非 400。

边界同读侧：只约束 ktorm DAO 这条路，手写 JDBC 仍然绕得过去。

### 加密列不能被静默地查错

`encryptedVarchar` 列的查询参数会先被同一个 codec 加密再送到数据库，所以
`WHERE secret = '已知值'` 实际是**密文比密文**。默认的 AES-GCM 每次 IV 不同，于是这个比较永远
不成立——查询**返回零行且不报任何错**，看起来就像"查无此人"。排序和 `LIKE` 更糟：它们会返回行，
只不过是按密文的顺序或形状筛出来的，像一个能用的查询，其实不是。

现在这些由 `EncryptedColumnGuard` **直接拒绝**并说明该怎么办：

| 操作 | 加密列上是否可用 |
|---|---|
| `IS_NULL` / `IS_NOT_NULL` | 总是可用（是否为 null 没有被加密） |
| `EQ` / `NE` / `IN` / `NOT_IN` / `IS_EMPTY` 等精确匹配 | **仅当 codec 是确定性的** |
| `LIKE` / `ILIKE` / `IEQ` / 大小比较 / `BETWEEN` | 永远不可用（数据库只看得到密文） |
| `ORDER BY` | 永远不可用（确定性也救不了：密文序与明文序无关） |

是否确定性由 `IStringEncryptionCodec.deterministic` 声明，**默认 false**——没想过这件事的
codec 作者会拿到安全的那个答案，而不是静默失效的查询。把它改成 true 之前请想清楚代价：相同明文
会产生相同密文，等于把值的分布泄露给任何能读这张表的人。需要"既加密又可查"时，另建一个确定性
哈希列去查。

守卫覆盖 DAO 构建的条件与排序（`Criteria`、search payload、`Order`）。确实要匹配原始密文的场景
（比如数据迁移）仍可直接用 Ktorm 自己的 DSL，那条路不经过这里。

### 分页必须有全序

`LIMIT` / `OFFSET` 是在数据库**恰好**产出的那个顺序上切片，而没有任何东西保证它两次产出同一个
顺序（PostgreSQL 明确不保证；并发写入下换个执行计划就会变）。按非唯一列排序也一样，并列的那组
内部顺序仍是不定的。后果是同一行出现在两页上，另一行一页都不出现。

因此**分页查询会自动追加主键作为 tiebreaker**（调用方已按主键排序时不重复追加），三条分页路径
（`pagingSearch` / `prepareQuery` / `search(payload)`）行为一致。未分页的查询不受影响。

### 批量操作要求形状一致

一个批次会变成**一条** prepared statement，因此其中每个实体必须设置相同的属性集合。旧实现按
首个实体推导列清单再过滤其余实体，于是后续实体多设置的属性被静默丢弃、少设置的则触发 Ktorm
那句不指名的 "must generate the same SQL"。现在 `batchInsert` / `batchInsertExclude` /
`batchUpdate` 会**提前拒绝**并指出差异的属性名。确实想固定列清单时用 `...Only` / `...Exclude`
系列显式指定。

### 空 IN 集合

`x IN ()` 在本框架支持的数据库上都不是合法 SQL，所以空集合要被"回答"而不是被"渲染"——答案是
没有任何行。`inSearch` / `inSearchById` / `inSearchProperty*` 系列均已如此处理（`getByIds`
原本就有这个防守）。

### Database 的缓存

Ktorm 的 `Database` 在构造时会借一条池化连接、发约 15 次 `DatabaseMetaData` 调用去读产品名、
保留字表、标识符引号与大小写折叠规则。旧实现把 `Database` 缓存在**请求级** `KudosContext` 里，
等于每个请求都付一次连接借用 + 往返。

现在按**路由解析后的物理 DataSource** 做进程级缓存。不做成全局单例是因为那些元数据描述的是连接
实际到达的那个库：本框架自己就存在一个 `DynamicRoutingDataSource` 同时路由到 PostgreSQL 主库和
ClickHouse 审计库的场景，共用一个 `Database` 会把一个产品的引号规则用到另一个产品的 SQL 上。

缓存用**弱键**，所以运行期被替换掉的数据源（管理员改 `sys_datasource` 后
`DsContextProcessor.refreshDatasource` 会重建实例）会连同它的 `Database` 一起被回收，**不需要
任何失效接线**：刷新产生的是新实例、也就是新键，旧的从来不会被取到。曾考虑订阅 jdbc 那条
cache-clean 信号来清理，但那要给本模块新增一个模块依赖、还多一个需要保持同步的钩子；弱键不需要
这些，也能覆盖其它途径的替换。代价是一次 map 查找上的锁，而这条路径下一步就是数据库往返。
`clearKtormDatabaseCache()` 保留给测试用，生产不需要记得调它。

注意 `Database` 包装的始终是**路由** DataSource 而非解析后的那个——Spring 的
`TransactionManager` 绑定在路由 bean 上，连接必须从它取才能加入当前事务，这正是本文开头那个
Seata 历史 bug。`KudosContext` 里显式放入的 `Database` 仍然优先（代码生成器和测试靠它指定库）。

### DataScopeContext 的线程边界

系统权限标记**只作用于设置它的那个线程**，跨线程要显式 `DataScopeContext.wrap(...)`。
这里刻意没有用 `InheritableThreadLocal`：继承发生在线程**创建**时，一个恰好在
`runAsSystem { }` 块里被创建出来的池化线程，会在其整个生命周期内一直带着系统权限去服务其它
请求——这种失败是静默的、永久的、无法按需复现的。相比之下，忘了 `wrap` 会在第一次调用时就
大声报错。

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
- `RowScopeEndToEndTest`：行过滤的组合规则（维度间 AND、维度内 OR、SELF 的 OR）
- `RowScopeDaoCoverageTest`：**行过滤的覆盖矩阵**——每个公开 DAO 入口一条断言。新增 DAO
  方法请一并在此加一行，否则"这个方法有没有过滤"就只能靠读代码逐个确认
- `DataScopeContextTest`：系统权限标记的线程边界（不自动跨线程；`wrap` 跨线程且不残留）
- `DaoGenericChainResolutionTest`：中间抽象 DAO / 代理多出一层时的泛型解析
- `EncryptedColumnGuardTest`：加密列上哪些查询被允许，以及 DAO 是否真的去问了守卫
- `KtormDatabaseCacheTest`：`Database` 按物理数据源缓存，且上下文里显式指定的优先

依赖 testcontainers postgres / H2，需要 Docker 运行环境。

## 已知限制 / 后续工作

- ✅ `KtormSqlType.getFunName()` 已支持具体枚举子类（如 `MyEnum::class`）映射到 `enum`；
  未映射的类型现在**抛异常并指名该类型**，而不是返回 `""`（那会让代码生成器写出
  `Schema.("col")` 这样的语法错误产物，且无从回溯到源头）。想判断是否支持用
  `getFunNameOrNull()`
- ✅ DAO 的泛型解析会**沿父类链向上并做类型变量替换**，因此中间隔一层抽象 DAO
  （`class OrderDao : AbstractTenantDao<Order, Orders>()`）以及 Spring CGLIB 代理多出的那一层
  都能正确解析。三个类型参数中任何一个没有落到具体类型时会明确报错，而不是退化成擦除后的上界
- ❗ `Database.connectWithSpringSupport(..., alwaysQuoteIdentifiers = true)` 会把每个标识符
  都加引号——PostgreSQL 下意味着区分大小写，建表时务必全用小写下划线命名（kudos 约定如此），
  否则会出现 `column "MyCol" does not exist` 之类的错
- ❗ `IntIdTable` / `LongIdTable` / `StringIdTable` 写死 `id` 列名 + 单列主键，多列主键
  / 自定义主键名不支持。需扩展时建议另起一套 `CompositeKeyTable`
- ❗ DAO 没有内置的乐观锁 / 软删除支持；业务侧需要时通过 `updateOnlyWhen` + 自定义
  `Criteria` / `whereConditionFactory` 自己组装
- ❗ `getOrderSql` 在 jdbc 模块对单引号做了过滤，本模块的 `sortOf` / `sortBy` 走的是 Ktorm
  `Column.asc()` / `desc()`，属性名由 `ColumnHelper` 解析失败即抛错（不会拼到 SQL 中），
  因此不需要另做注入过滤
- ❗ **行过滤只约束本模块**。直接走 `kudos-ability-data-rdb-jdbc` 手写 SQL 的代码不受
  `RowScopeEnforcer` 管辖。想让其它数据访问路径也接入，需要把 rowscope 的契约（注解 /
  `DataScopeContext` / `IRowScopeResolver`）上移到与 ORM 无关的模块
- ✅ **写入值校验已实现**（见下节）：insert 不能建到自己范围外，update 不能把行移出自己范围
- ❗ `BaseReadOnlyDao` 单文件较长，`payload search` / `aggregate` / `processWhere` 各自独立，
  可拆成多个 helper object 提升可读性——历史代码，迁移成本不低，暂搁置
- ❗ `search(listSearchPayload): List<*>` 的三态返回**仍然保留**（见下节），改它会波及每一个
  ms-* service。新代码请改用有明确返回类型的那几个方法

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
