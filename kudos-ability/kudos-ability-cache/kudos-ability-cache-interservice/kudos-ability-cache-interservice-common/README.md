# kudos-ability-cache-interservice-common

跨服务缓存协作三件套（common / provider / client）的**共享基**模块。承载 provider
与 client 双端共用的 DTO、缓存键 / 头协议常量。

## 解决的问题

> provider 应用内部缓存了一份数据，client 应用通过 Feign 调过来读；同一份数据被反复
> 序列化 / 网络传输。能不能在 HTTP 层面引入"协商缓存"，让 client 端用 UID 指纹判断本地
> 副本是否仍然有效？

整体类似 HTTP ETag / If-None-Match 的"双方缓存共识"——但是把指纹计算下沉到应用层
（基于响应对象 JSON），不依赖 HTTP 缓存控制头。

## 模块入口

| 路径 | 角色 |
|---|---|
| `common/ClientCacheItem` | 缓存条目 DTO：`uuid`（响应对象的内容指纹）+ `cacheData`（响应内容本体）。`genUid(obj)` 静态方法基于 `<FQN>#<JSON>` 算 MD5 |
| `common/ClientCacheKey` | 缓存键 DTO + 协议常量（`HEADER_KEY_CACHE_UID` / `HEADER_KEY_CACHE_KEY` / `HEADER_KEY_CACHE_STATUS` / `STATUS_USE_CACHE=304` / `STATUS_DO_CACHE=200`）。`toString()` 拼接 `url::method::body` 形成签名段 |

## 协议契约（client ↔ provider）

```
请求阶段（client → provider）:
  cache-key:    md5(":: + tenantId + :: + appName + url::method::body")
  cache-uid:    本地缓存项的 uuid（若有）

响应阶段（provider → client）:
  cache-uid:    服务端生成的 uuid（基于 FQN+JSON 的 MD5）
  cache-status: 304（client 直接用本地缓存）/ 200（client 用 body 并写本地缓存）
```

## 指纹稳定性约定

`ClientCacheItem.genUid(obj)` 把 FQN 与 JSON 用 `#` 分隔后再算 MD5，避免"类名 + JSON
直接拼接产生同字符串"的边角碰撞。MD5 在这里**仅作内容指纹**，与加密无关。

**已知风险**：DTO 中带 `Map<*, *>` 且非 `LinkedHashMap` 时迭代顺序不稳定，会导致 JSON
不稳 → UID 抖动。接口层应避免直接返回原始 `Map`，或显式用 `LinkedHashMap` / 排序后返回。

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-common"))
```

`cache-common` 提供 `IKeyValueCacheManager`——client 端的 `ClientCacheHelper` 用它作为
本地 KV 存储。

## 已知限制

- ❗ 模块名 `interservice` 中文术语未统一——"服务间缓存" / "跨服务缓存" / "跨应用缓存"
  三种叫法并存，未来收敛到一种为好
- ❗ `ClientCacheItem` 用 `Serializable` + 默认 JVM 序列化——若以后跨节点广播缓存项
  需要切换到 JSON 序列化的话要同时调整 `serialVersionUID` 兼容策略
