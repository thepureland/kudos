# kudos-ams-sys-core

系统（sys）域核心实现模块，提供领域模型、DAO、Service 与缓存处理器。

## 主要内容

- `io.kudos.ams.sys.core.model`
  - `po` / `table`（Ktorm 实体与表映射）
- `io.kudos.ams.sys.core.dao`
  - Ktorm DAO
- `io.kudos.ams.sys.core.service`
  - 业务服务（`impl` + `iservice`）
- `io.kudos.ams.sys.core.cache`
  - 缓存处理器（*CacheHandler）
- `io.kudos.ams.sys.core.api`
  - 领域 API 实现
- `io.kudos.ams.sys.core.init`
  - `SysAutoConfiguration`

## 依赖

- Ktorm / Flyway
- Cache（local/redis）
- `kudos-ams-sys-common`

## 数据库脚本

位置：

```
resources/sql/sys/h2
```

## 测试

- 使用 `kudos-test-rdb`
- 已配置 H2 / PostgreSQL 依赖
