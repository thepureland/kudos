# kudos-ms-auth-api-internal

Auth 服务**对内 Provider 进程**的启动入口与自动配置。

## 内容

- `AuthApiProviderApplication` —— Spring Boot `main()`
- 与 `api-public` 对称——但作为微服务间 Feign 调用的 provider，配独立的网络入口
- 通常关闭面向最终用户的 web 路径，只保留对内 API

## 部署形态

跑两个独立 JVM：
- `api-public` 对外暴露管理端 HTTP
- `api-internal` 给其他微服务（如 user / sys / msg）通过 Feign 调用

这种分离让"控制台流量"与"服务间流量"在网络层 / 监控 / SLI 上能分别治理。

## 依赖

同 `api-public`：core + api-admin（其实 internal 通常不挂 admin controller，但代码里仍引入
以共享 Spring 装配）+ web 栈。

## 已知限制

- ❗ `api-internal` 与 `api-public` 的实际边界目前由 yml 配置决定（端口 / actuator 暴露面），
  代码层未硬分隔——业务侧需自行约束哪些路径"内部专用"
