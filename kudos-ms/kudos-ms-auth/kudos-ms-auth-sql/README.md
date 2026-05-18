# kudos-ms-auth-sql

Auth 原子服务的 Flyway 迁移脚本——只放 `*.sql`，无 Kotlin 代码。

## 表结构

| 表 | 说明 |
|---|---|
| `auth_role` | 角色主数据 |
| `auth_role_resource` | 角色 ↔ 资源关联（资源 id 指向 `sys.sys_resource`） |
| `auth_role_user` | 角色 ↔ 用户关联（用户 id 指向 `user.sys_user`） |
| `auth_group` | 用户组主数据，带 path 字段表层级 |
| `auth_group_user` | 组 ↔ 用户关联 |

## 约定

- 表前缀 `auth_`——与 sys / user / msg 服务隔离
- Flyway baseline + V_*_* 命名风格
- 一个 SQL 文件一条迁移；不在一个文件里混多个 DDL/DML
- 内置数据（如系统角色）走 `R_*` 可重复脚本

## 依赖

无 Kotlin 依赖，纯资源模块。`auth-core` 在测试 / 部署时引入本模块的 SQL 给 Flyway 跑。
