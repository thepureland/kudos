# kudos-ms-user-api-internal

User 服务**对内 Provider 进程**的启动入口与自动配置。

## 内容

- `UserApiProviderApplication` —— Spring Boot main
- 与 api-public 对称，但作为微服务间 Feign 调用的 provider

## 部署形态

- api-public：管理端 / 用户面向的 HTTP
- api-internal：服务间 Feign 调用入口

通过 yml 区分两者的端口、actuator 暴露面、网络可见性。

## 依赖

同 api-public——core + api-admin + web 栈。

## 已知限制

- ❗ api-internal 与 api-public 的边界由 yml 决定，代码层未硬分隔
