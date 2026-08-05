# kudos-ability-cache-interservice

跨服务（inter-service）缓存协作三件套。在 Feign 调用链上实现类似 HTTP ETag /
If-None-Match 的"协商缓存"：client 端携带本地缓存指纹（UID），provider 端比对后
返回 `304`（复用本地副本）或 `200`（返回新数据并更新本地缓存）。

| 子模块 | 说明 |
|---|---|
| [`kudos-ability-cache-interservice-common`](kudos-ability-cache-interservice-common/README.md) | 共享契约：`ClientCacheItem`（UID + 数据）、`ClientCacheKey`（键 / 请求头协议常量） |
| [`kudos-ability-cache-interservice-provider`](kudos-ability-cache-interservice-provider/README.md) | provider 端：`@ClientCacheable` 注解 + Aspect、`ClientCacheWebFilter` 请求包装 |
| [`kudos-ability-cache-interservice-client`](kudos-ability-cache-interservice-client/README.md) | client 端：Feign Request/Response 拦截器 + 本地缓存 helper |

详情和已知问题见各子模块 README。
