# kudos-ability-data-tsdb

时间序列数据库能力主题。

| 子目录 | 内容 |
|---|---|
| [`kudos-ability-data-tsdb-influxdb`](kudos-ability-data-tsdb-influxdb/README.md) | InfluxDB 2.x 基础支持（单数据源 MVP） |

适用场景：监控指标 / IoT 设备时序数据 / 高频事件流。业务工程一般不需要 TSDB；当你确认
需要"按时间点写入、按时间范围聚合查询"的数据特征时才引入。
