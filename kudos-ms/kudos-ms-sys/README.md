# kudos-ms-sys

作为所有原子服务的基础，从底层和全局的角度，定义治理标准，充当元数据注册中心，统一管理各项基础资源。

## 概念
1.系统(system)
- 面向某类业务目标的一组能力集合，对外呈现为一个相对完整的“应用/产品单元”
- 一个或多个系统组成一个平台/产品/项目/应用
- 通常会有用户界面

2.子系统(sub-system)：
- 系统内部的业务能力分区（逻辑边界），用于拆分职责与降低复杂度
- 本质上也是一个系统，也就是说仍然是完整的实体。系统和子系统的概念是相对的，当作为另一个系统的一部分时，系统就成为一个子系统
- 一个或多个子系统组成一个系统
- 通常会有用户界面

3.微服务(micro service)：
- 实现业务能力的可独立部署运行单元（部署边界），通过 API/消息互通
- 一个或多个微服务组成一个子系统
- 不一定有用户界面

4.原子服务(atomic service)：
- 实现业务能力的最小可独立部署运行单元（部署边界），通过 API/消息互通。
- 本质上也是一个微服务，特指不可再拆分的微服务，多个原子服务可以轻松组成或脱离一个微服务
- 一个或多个原子服务组成一个微服务
- 开发时都会按原子服务的粒度进行开发，部署时才会视需求组合成微服务
- 不一定有用户界面

5.租户
- 不同的运营主体，在系统/子系统维度上的数据隔离
- 租户可以是一个团队



## 模块结构

- `kudos-ms-sys-core`
  - 领域核心实现：DAO、Service、缓存处理器、领域模型
  - 依赖：ktorm + flyway + cache（local/redis）
  - 组件初始化：`io.kudos.ms.sys.core.init.SysAutoConfiguration`
- `kudos-ms-sys-common`
  - 领域 VO 与 API 接口（`io.kudos.ms.sys.common.*`）
- `kudos-ms-sys-client`
  - Feign 接口与 fallback（`io.kudos.ms.sys.client.*`）
- `kudos-ms-sys-api-public`
  - 面向外部的 Spring MVC API 应用
  - 启动类：`io.kudos.ms.sys.api.public.init.SysApiWebApplication`
- `kudos-ms-sys-api-admin`
  - 面向管理端的 Spring MVC API 应用
  - 启动类：`io.kudos.ms.sys.api.admin.init.SysApiAdminApplication`
- `kudos-ms-sys-api-internal`
  - 内部服务间调用的 Spring MVC API 应用
  - 启动类：`io.kudos.ms.sys.api.internal.init.SysApiProviderApplication`
