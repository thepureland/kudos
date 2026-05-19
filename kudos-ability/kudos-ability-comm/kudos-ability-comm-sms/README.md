# kudos-ability-comm-sms

短信发送实现集合——按云厂商分模块。

| 子模块 | 实现 |
|---|---|
| [`kudos-ability-comm-sms-aliyun`](kudos-ability-comm-sms-aliyun/README.md) | 阿里云 dysmsapi。**当前在 settings.gradle.kts 被注释停用**——见子模块 README |
| [`kudos-ability-comm-sms-aws`](kudos-ability-comm-sms-aws/README.md) | AWS SNS |

两个实现都用 WireMock 集成测试 + 走虚拟线程异步发送。
