# kudos-ams-sys

系统（sys）原子服务，提供系统、子系统、微服务、原子服务、资源、字典、参数、租户等基础配置与能力。

## 模块结构

- `kudos-ams-sys-core`
  - 领域核心实现：DAO、Service、缓存处理器、领域模型
  - 依赖：ktorm + flyway + cache（local/redis）
  - 组件初始化：`io.kudos.ams.sys.core.init.SysAutoConfiguration`
- `kudos-ams-sys-common`
  - 领域 VO 与 API 接口（`io.kudos.ams.sys.common.*`）
- `kudos-ams-sys-client`
  - Feign 接口与 fallback（`io.kudos.ams.sys.client.*`）
- `kudos-ams-sys-api-public`
  - 面向外部的 Spring MVC API 应用
  - 启动类：`io.kudos.ams.sys.api.public.init.SysApiWebApplication`
- `kudos-ams-sys-api-admin`
  - 面向管理端的 Spring MVC API 应用
  - 启动类：`io.kudos.ams.sys.api.admin.init.SysApiAdminApplication`
- `kudos-ams-sys-api-internal`
  - 内部服务间调用的 Spring MVC API 应用
  - 启动类：`io.kudos.ams.sys.api.internal.init.SysApiProviderApplication`
