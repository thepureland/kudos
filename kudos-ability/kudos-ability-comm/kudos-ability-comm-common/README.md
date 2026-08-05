# kudos-ability-comm-common

通信类模块（email / sms / websocket）的共享 base。**当前几乎为占位**——
只有一个未被任何模块使用的 `CommThreadPoolProperties` 配置类；`build.gradle` 仅声明
`api(project(":kudos-context"))` 一条依赖。

## 当前状态

```
src/io/kudos/ability/comm/common/init/properties/CommThreadPoolProperties.kt
```

`CommThreadPoolProperties` 是为"共享同步发送线程池"预留的——但 comm-email、
comm-sms-aliyun、comm-sms-aws 全部走虚拟线程，不需要这个池。所以本配置类**当前没有
被任何模块装配**。

## 已知限制

- ❗ **本质上是空模块**。可选处理：
  - 等到真有共享代码再保留（不太可能——大势是虚拟线程）
  - 删除 `CommThreadPoolProperties` 然后整个目录删掉，让 `comm-email` 等模块直接依赖
    `kudos-context`
- ❗ 模块只有一个无人装配的配置类，`build.gradle` 仅透传 `kudos-context` 依赖（settings 仍声明本模块）
- ❗ 与 `kudos-ability-cache-interservice-common` / `web-common` / `log-audit-rdb-common`
  同款占位风格；建议批量决策一起处理
