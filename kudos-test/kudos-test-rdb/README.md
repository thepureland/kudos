# kudos-test-rdb

RDB 测试基类——给业务测试一套"启 RDB 容器 + 装数据 + 重置缓存"的模板。

## 类

| 类 | 角色 |
|---|---|
| `SqlTestBase` | 单纯执行 SQL 脚本初始化数据 |
| `RdbTestBase` | RDB + flyway / 数据初始化基类 |
| `RdbAndRedisCacheTestBase` | RDB + Redis 双容器测试基类 |
| `CacheTestResetSupport` | 测试间清空缓存的工具 |

## 使用模式

业务测试继承相应基类即可，启动逻辑统一在基类中。
