# kudos-ability-cache-local

本地（同进程）缓存实现集合。

| 子模块 | 实现 |
|---|---|
| [`kudos-ability-cache-local-caffeine`](kudos-ability-cache-local-caffeine/README.md) | Caffeine——目前唯一实现 |

设计上预留对接其他本地缓存（如 EhCache）的位置，但当前仅 Caffeine 一种。
