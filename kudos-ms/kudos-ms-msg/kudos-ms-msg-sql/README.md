# kudos-ms-msg-sql

Msg 原子服务的 **Flyway 资源模块**——只放 `*.sql`，没有 Kotlin 编译产物。
`build.gradle.kts` 是空 `dependencies { }`，靠消费侧（`msg-api-public` / `msg-api-internal`）
的 Flyway 配置在 boot 时扫描 `classpath:sql/msg/{dialect}/`。

## 目录结构

```
resources/sql/msg/
├── h2/                    ← 测试 / 本地
├── postgresql/            ← 与 h2 版本号一一对齐
└── mysql/                 ← 与 h2 版本号一一对齐（oracle 尚未提供）
    各方言内：
    ├── V1.0.0.0..6        ← 系统配置 / 字典 / i18n 引导
    ├── V1.0.0.20..25      ← msg_* 业务表 DDL
    ├── V1.0.0.26          ← msg_send 幂等键 alter
    (中间 V1.0.0.7..19 保留给后续脚本)
```

运行时由 Flyway 按 `RdbTypeEnum` 自动选择方言目录；DML 侧 PG 用 `on conflict do nothing`、
MySQL 用 `insert ignore` 保证幂等（详见组级 README"整体已知限制"）。

## 迁移清单

| 文件 | 作用 |
|------|------|
| `V1.0.0.0__insert_sys_micro_service.sql` | 注册 `msg` 微服务（`atomic_service=true`、`context=/api/msg`、`built_in=true`） |
| `V1.0.0.1__insert_sys_dict.sql` | 创建 8 个字典类型：`publish_method` / `receiver_group_type` / `send_status` / `tmpl_type` / `auto_event_type` / `manual_event_type` / `params` / `receive_status` |
| `V1.0.0.2__insert_sys_dict_item.sql` | 字典项落库——见下方"字典与枚举对齐" |
| `V1.0.0.3__insert_sys_cache.sql` | ⚠️ **整文件被注释**——`USER_ORG_IDS_BY_USER_ID` 缓存定义保留为占位，待启用 |
| `V1.0.0.4__insert_sys_resource.sql` | ⚠️ **仅一行注释 "消息中心 Bell,"**——sys_resource 入库尚未写 |
| `V1.0.0.5__insert_sys_param.sql` | ⚠️ **空文件**——msg 服务暂无 sys_param 默认值 |
| `V1.0.0.6__insert_sys_i18n.sql` | 字典名/字典项的 `zh_CN` / `zh_TW` / `en_US` 三语翻译（约 160 行） |
| `V1.0.0.20__init_msg_template.sql` | `msg_template` DDL |
| `V1.0.0.21__init_msg_instance.sql` | `msg_instance` DDL（FK → `msg_template.id`） |
| `V1.0.0.22__init_msg_receiver_group.sql` | `msg_receiver_group` DDL（含完整审计列、`uq_msg_receiver_group__type_code` 唯一索引） |
| `V1.0.0.23__init_msg_send.sql` | `msg_send` DDL（FK → `msg_instance.id`） |
| `V1.0.0.24__init_msg_receive.sql` | `msg_receive` DDL（FK → `msg_send.id`） |
| `V1.0.0.25__init_msg_unreceived.sql` | `msg_unreceived` DDL（FK → `msg_send.id`，含 `idx_msg_unreceived_send` / `idx_msg_unreceived_receiver_unresolved`） |
| `V1.0.0.26__alter_msg_send_idempotency.sql` | 给 `msg_send` 加 `idempotency_key` 列 + 唯一索引 `uq_msg_send__tenant_idempotency`（`tenant_id` + `idempotency_key`），支撑 publish 幂等去重 |

## 表关系

```
msg_template ──┐
               │ template_id (nullable)
               ▼
       msg_instance ──┐
                      │ instance_id (NOT NULL)
                      ▼
              msg_send ──┬─→ msg_receive    (一收件人一行)
                         └─→ msg_unreceived (失败补偿，含 retry_count / resolved)

msg_receiver_group ── 独立 lookup 表，
                     msg_send.receiver_group_id 是 VARCHAR(36) 软引用，
                     未建 FK，由应用层保证一致性。
```

## 字典与枚举对齐

| dict_type | 字典项 | 对应 Kotlin 枚举 |
|-----------|--------|------------------|
| `publish_method` | `email` / `sms` / `siteMsg` / `all_user` | `MsgPublishMethodEnum` 完整对齐 |
| `receiver_group_type` | `all_front` / `all_back` / `online_front` / `online_back` / `offline_front` / `offline_back` / `dept` / `role` / `tag` / `guest` / `user` (11 项) | （无独立枚举，运行时按字符串处理） |
| `send_status` | `00` / `01` / `11` / `21` / `22` / `31` / `32` / `33` | `MsgSendStatusEnum` 完整对齐 |
| `receive_status` | `01` / `11` / `12` / `21` | `MsgReceiveStatusEnum` 完整对齐 |
| `tmpl_type` | `auto` / `manual` | （无枚举，字符串） |

**字典码与枚举耦合**：每次改 Kotlin 枚举的 `dictCode` 都得同步 SQL，否则运行时拿到字符串
匹配不上枚举。建议两边都加 unit test 校验集合相等。

## 已知限制 / 后续工作

- ✅ **方言已补 PG + MySQL**——`h2` / `postgresql` / `mysql` 三套并存、版本号对齐；
  oracle 尚未提供。h2 仍用 `RANDOM_UUID()`、无界 `varchar` 等方言语法，仅供测试 / 本地
- ✅ **`msg_template.content` 列类型已按方言落地**——PG / MySQL 版用 `text`，配合应用层
  `IMsgTemplateFormBase` 的 `@MaxLength(65535)` 校验封顶；h2 的无界 `varchar` 仅测试用
- ❗ **部分索引仍缺**：`idx_msg_template__tenant_event` 已补在 PG / MySQL（h2 暂未补）；
  但 `msg_instance`（template_id / tenant_id）、`msg_send`（tenant_id /
  send_status_dict_code）、`msg_receive`（receiver_id / receive_status）等索引在三套方言
  中均未建。状态查询 / 多租户隔离查询 / 收件箱查询会全表扫
- ❗ **审计列不一致**：`msg_receiver_group` 有完整 `create_user_id` / `create_user_name` /
  `create_time` / `update_*` 六列；其他 5 张 msg_* 表只有 `create_time` / `update_time`，
  缺操作人。合规审计场景需先补列
- ❗ **`msg_send.receiver_group_id` 是 VARCHAR(36)，未对 `msg_receiver_group.id`（CHAR(36)）
  建 FK**——类型不一致 + 无约束，应用层若写入错误 id 不会立即报错
- ❗ **时间戳精度不一致**：`msg_template` / `msg_instance` 用 `TIMESTAMP`，其他用 `TIMESTAMP(6)`
- ❗ **`msg_instance.valid_time_end` 默认 `now()+99999`**——h2 把这个数字按"天"算（约 273 年），
  跨方言移植语义会改变，建议显式给业务侧默认值
- ❗ **无软删列**——全部物理删；接收记录被删后无法做"用户曾经收到过这条消息"的合规追溯
- ❗ **V1.0.0.3 / V1.0.0.4 / V1.0.0.5 三脚本都是空 / 占位**——文件已占版本号，后续真要写内容
  需新版本号（`V1.0.0.26+`），不能复用

## 约定

- 表前缀 `msg_`
- 版本号空间：`V1.0.0.0..6` 给系统数据 / 字典，`V1.0.0.20+` 给业务表 DDL，中间留 hole
- 命名：`fk_msg_<child>`、`idx_msg_<table>_<purpose>`、`uq_msg_<table>__<columns>`
- 内置模板 / R_*\_\* 可重复脚本：当前尚未引入，欢迎消息等首批模板由 msg-core 服务层
  按 boot 初始化（待确认）

## 依赖

无 Kotlin 依赖，纯资源模块。
