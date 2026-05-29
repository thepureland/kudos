# kudos-ability-web-swagger

基于 [springdoc-openapi](https://springdoc.org/) 3.x 的 OpenAPI 3.0 文档支持。

业务工程把本模块作为依赖加入即可获得：

1. **统一的 [OpenAPI] bean**：由 `kudos.ability.web.swagger.*` yml 字段驱动 title / description /
   version / contact 信息；apps 直接拿到 `/v3/api-docs` 端点
2. **可关闭的总开关**：`kudos.ability.web.swagger.enabled=false` 不装配 OpenAPI bean，
   不需要在依赖里把模块整个排掉
3. **production 提示位**：`kudos.ability.web.swagger.production=true` 不会关掉文档生成（build
   阶段 SDK codegen 还要用），只是一个可被下游过滤器（HTTP 网关、UI 拦截）读取的标记

## 设计要点

### 为什么默认不带 Swagger UI

`springdoc-openapi-starter-webmvc-api` 只提供 OpenAPI 元数据 + `/v3/api-docs` JSON 端点，
**不含** swagger-ui 静态资源。需要可视化页面的业务工程自行 import
`org.springdoc:springdoc-openapi-starter-webmvc-ui`——这样生产构建不被强行塞进 UI 静态文件。

### 为什么没有 knife4j 集成

[Soul 的 `Swagger3Configuration`](https://github.com/itboot/soul) 同时注册了一个
`SoulOpenAPIService extends OpenAPIService`，用来把 knife4j 的 `@ApiSupport(order = N)` 注解
映射成 OpenAPI tag 的 `x-order` extension（用于自定义 UI 模块排序）。该扩展仅在使用
knife4j UI 时有意义；kudos 当前没有 knife4j 业务诉求，因此**不强制把 knife4j 拉进 classpath**。
将来某个 app 真正接入 knife4j 时，再补一个独立的 `kudos-ability-web-swagger-knife4j`
sub-module 即可。

### 与 web-springmvc 的关系

本模块依赖 `springdoc-openapi-starter-webmvc-api`（已 transitively 引入 `spring-web` /
`spring-webmvc`）。Spring MVC 应用按 `springdoc-openapi-starter-webmvc-api` 的 starter
自动配置会扫描 `@RestController` 并生成路径分组；本模块只负责文档的 `info` 块和
开关属性。Ktor 应用走另一套 OpenAPI 路径（`ktor-server-openapi`），与本模块不相干。

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/SwaggerAutoConfiguration` | 装配入口；条件性发布 `OpenAPI` bean |
| `init/properties/SwaggerProperties` | yml 绑定根；`kudos.ability.web.swagger.*` |
| `init/properties/SwaggerContact` | maintainer 联系信息子节点 |
| `resources/kudos-ability-web-swagger.yml` | 模块默认值；业务 yml 按需覆盖 |

## 配置示例

```yaml
kudos:
  ability:
    web:
      swagger:
        enabled: true
        production: false
        title: My App API
        description: REST API for My App
        version: 1.0.0
        url: https://docs.example.com
        contact:
          contact-name: Platform Team
          contact-url: https://example.com/team
          contact-email: platform@example.com

# springdoc 自身的配置可继续按 springdoc 文档使用
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:        # 仅当业务工程额外引入 -ui 子 starter 时生效
    path: /swagger-ui.html
```

## 业务接入 Swagger UI（可选）

```kotlin
// build.gradle.kts of business app
dependencies {
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-swagger"))
    api(libs.springdoc.openapi.starter.webmvc.ui)   // 额外引入 UI bundle
}
```

## 自定义 OpenAPI bean

业务工程如果需要完整文档（含 security schemes / multiple servers / external docs），
直接声明自己的 `OpenAPI` bean，本模块的 `@ConditionalOnMissingBean` 会自动让位：

```kotlin
@Configuration
class CustomOpenAPIConfiguration {
    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(Info().title("Custom").version("2.0.0"))
        .components(Components().addSecuritySchemes("bearer", SecurityScheme()
            .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
        .addSecurityItem(SecurityRequirement().addList("bearer"))
}
```

## 测试覆盖

- `SwaggerContactTest` —— 默认全 null 字段
- `SwaggerPropertiesTest` —— 默认值（`enabled=true` / `production=false` / 字段全 null）
- `SwaggerAutoConfigurationTest` —— 完整 Spring 上下文：yml 绑定 → OpenAPI bean 装配
  / `enabled=false` 时不装配 / 业务自定义 `OpenAPI` bean 时让位 / 默认值 OpenAPI 文档

未覆盖：springdoc 自身的 `/v3/api-docs` HTTP 端点行为（属 springdoc 责任）；
knife4j 扩展（未启用）。

## 已知限制 / 后续工作

- ❗ `groupName` 字段为 forward-compat 占位，目前不映射到任何 `GroupedOpenApi` bean
- ❗ `production` flag 仅是属性位，不连接任何过滤器；业务工程需自己读取并对
  `/v3/api-docs` 加权限拦截，或下游网关层屏蔽
- ❗ Knife4j tag-order 扩展未移植；接入 knife4j 的工程需自己注入
  `OpenAPIService` 子类

## 依赖

```kotlin
api(project(":kudos-context"))
api(libs.springdoc.openapi.starter.webmvc.api)

testImplementation(project(":kudos-test:kudos-test-common"))
testImplementation(libs.spring.boot.starter.web)
testImplementation(libs.spring.boot.starter.webmvc.test)
testImplementation(libs.spring.boot.starter.jetty)
```
