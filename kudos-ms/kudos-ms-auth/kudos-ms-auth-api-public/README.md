# kudos-ms-auth-api-public

Auth 服务**对外 Web 进程**的启动入口与自动配置。

## 内容

- `AuthApiWebApplication` —— Spring Boot `main()`
- 自动配置：装载 auth-core + auth-api-admin + web 全栈（springmvc + filter + interceptor）
- 默认监听管理端 HTTP 路径

## 启动

```bash
java -jar auth-api-public-<version>.jar
```

或本仓库测试：

```bash
./gradlew :kudos-ms:kudos-ms-auth:kudos-ms-auth-api-public:bootRun
```

## 依赖

- `kudos-ms-auth-api-admin`（controllers）
- `kudos-ms-auth-core`（业务实现）
- `kudos-ability-web-springmvc`（web 栈）
- Spring Boot starter 与各项 starter（cache / 数据库等通过 core 间接带入）

## 与 api-internal 的区别

| 维度 | api-public | api-internal |
|---|---|---|
| 暴露面 | 管理端 HTTP / 公开 web | 内网 Feign provider |
| 启用 controller | 全部 | 全部，但通常配独立网络入口 |
| 适用场景 | 用户 / 控制台 | 微服务间调用 |

具体差异看各自 `Application` + yml。
