# kudos-ms-user-api-public

User 服务**对外 Web 进程**的启动入口与自动配置。

## 内容

- `UserApiWebApplication` —— Spring Boot main
- 装配 user-core + user-api-admin + 完整 web 栈

## 启动

```bash
./gradlew :kudos-ms:kudos-ms-user:kudos-ms-user-api-public:bootRun
```

## 依赖

- `kudos-ms-user-api-admin`、`kudos-ms-user-core`
- `kudos-ability-web-springmvc`
- 其他 starter 通过 core 间接带入

## 与 api-internal 的区别

api-public 面向最终用户 / 控制台；api-internal 面向服务间 Feign 调用。代码层基本一样，
通过端口 / actuator 等 yml 配置进行实际分离。
