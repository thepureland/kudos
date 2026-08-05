package io.kudos.test.container.containers

import io.kudos.test.container.FakeDynamicPropertyRegistry
import io.kudos.test.container.containers.ContainerPortMocks.container
import io.kudos.test.container.containers.ContainerPortMocks.port
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit-tests every container object's `registerProperties(...)` against mocked docker-java
 * [com.github.dockerjava.api.model.Container] data, plus their `requireNotNull`/`error` guards.
 * No Docker daemon is involved.
 *
 * @author K
 * @since 1.0.0
 */
internal class RegisterPropertiesTest {

    @Test
    fun `redis registers kudos and spring properties`() {
        val reg = FakeDynamicPropertyRegistry()
        RedisTestContainer.registerProperties(reg, container(ports = arrayOf(port("127.0.0.1", 16379))))
        assertEquals("127.0.0.1", reg.value("kudos.ability.data.redis.redis-map.data.host"))
        assertEquals(16379, reg.value("kudos.ability.data.redis.redis-map.data.port"))
        assertEquals("127.0.0.1", reg.value("spring.data.redis.host"))
        assertEquals(16379, reg.value("spring.data.redis.port"))
    }

    @Test
    fun `redis fails when port ip is null`() {
        val reg = FakeDynamicPropertyRegistry()
        val ex = assertFailsWith<IllegalArgumentException> {
            RedisTestContainer.registerProperties(reg, container(ports = arrayOf(port(null, 16379))))
        }
        assertEquals("container port ip is null", ex.message)
    }

    @Test
    fun `redis fails when publicPort is null`() {
        val reg = FakeDynamicPropertyRegistry()
        val ex = assertFailsWith<IllegalArgumentException> {
            RedisTestContainer.registerProperties(reg, container(ports = arrayOf(port("127.0.0.1", null))))
        }
        assertEquals("container publicPort is null", ex.message)
    }

    @Test
    fun `h2 builds tcp jdbc url`() {
        val reg = FakeDynamicPropertyRegistry()
        H2TestContainer.registerProperties(reg, container(ports = arrayOf(port("10.0.0.5", 11521))))
        assertEquals(
            "jdbc:h2:tcp://10.0.0.5:11521/mem:${H2TestContainer.DATABASE};DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;",
            reg.value("spring.datasource.dynamic.datasource.h2.url")
        )
        assertEquals(H2TestContainer.USERNAME, reg.value("spring.datasource.dynamic.datasource.h2.username"))
        assertEquals(H2TestContainer.PASSWORD, reg.value("spring.datasource.dynamic.datasource.h2.password"))
    }

    @Test
    fun `kafka registers broker address`() {
        val reg = FakeDynamicPropertyRegistry()
        KafkaTestContainer.registerProperties(reg, container(ports = arrayOf(port("host1", 9092))))
        assertEquals("host1:9092", reg.value("spring.cloud.stream.kafka.binder.brokers"))
    }

    @Test
    fun `mysql builds jdbc url with charset params`() {
        val reg = FakeDynamicPropertyRegistry()
        MySqlTestContainer.registerProperties(reg, container(ports = arrayOf(port("db", 23306))))
        assertEquals(
            "jdbc:mysql://db:23306/${MySqlTestContainer.DATABASE}?useSSL=false&useUnicode=true&characterEncoding=utf8",
            reg.value("spring.datasource.dynamic.datasource.mysql.url")
        )
        assertEquals(MySqlTestContainer.USERNAME, reg.value("spring.datasource.dynamic.datasource.mysql.username"))
        assertEquals(MySqlTestContainer.PASSWORD, reg.value("spring.datasource.dynamic.datasource.mysql.password"))
    }

    @Test
    fun `mongo registers host port database and uri`() {
        val reg = FakeDynamicPropertyRegistry()
        MongoTestContainer.registerProperties(reg, container(ports = arrayOf(port("m", 27117))))
        assertEquals("m", reg.value("spring.data.mongodb.host"))
        assertEquals(27117, reg.value("spring.data.mongodb.port"))
        assertEquals("kudos-test", reg.value("spring.data.mongodb.database"))
        assertEquals("mongodb://m:27117/kudos-test", reg.value("spring.data.mongodb.uri"))
    }

    @Test
    fun `mongo fails when ip is null`() {
        val reg = FakeDynamicPropertyRegistry()
        assertFailsWith<IllegalArgumentException> {
            MongoTestContainer.registerProperties(reg, container(ports = arrayOf(port(null, 27117))))
        }
    }

    @Test
    fun `rabbitmq registers full amqp properties`() {
        val reg = FakeDynamicPropertyRegistry()
        RabbitMqTestContainer.registerProperties(reg, container(ports = arrayOf(port("rmq", 25672))))
        assertEquals("rmq", reg.value("spring.rabbitmq.host"))
        assertEquals("guest", reg.value("spring.rabbitmq.username"))
        assertEquals("guest", reg.value("spring.rabbitmq.password"))
        assertEquals(25672, reg.value("spring.rabbitmq.port"))
        assertEquals("/", reg.value("spring.rabbitmq.virtual-host"))
        assertEquals("rmq:25672", reg.value("spring.rabbitmq.addresses"))
    }

    @Test
    fun `rabbitmq fails when publicPort is null`() {
        val reg = FakeDynamicPropertyRegistry()
        assertFailsWith<IllegalArgumentException> {
            RabbitMqTestContainer.registerProperties(reg, container(ports = arrayOf(port("rmq", null))))
        }
    }

    @Test
    fun `minio registers identical internal and public endpoints`() {
        val reg = FakeDynamicPropertyRegistry()
        MinioTestContainer.registerProperties(reg, container(ports = arrayOf(port("minio", 19000))))
        assertEquals("http://minio:19000", reg.value("kudos.ability.file.minio.endpoint"))
        assertEquals("http://minio:19000", reg.value("kudos.ability.file.minio.public-endpoint"))
    }

    @Test
    fun `clickhouse picks the http 8123 port for jdbc url`() {
        val reg = FakeDynamicPropertyRegistry()
        // native(9000) listed first to prove the filter selects the 8123 mapping, not just the first port
        val c = container(
            ports = arrayOf(
                port("ch", 49000, privatePort = 9000),
                port("ch", 48123, privatePort = 8123)
            )
        )
        ClickHouseTestContainer.registerProperties(reg, c)
        assertEquals("jdbc:clickhouse://ch:48123/${ClickHouseTestContainer.DATABASE}", reg.value("kudos.test.clickhouse.jdbc-url"))
        assertEquals(ClickHouseTestContainer.USERNAME, reg.value("kudos.test.clickhouse.username"))
        assertEquals(ClickHouseTestContainer.PASSWORD, reg.value("kudos.test.clickhouse.password"))
        assertEquals(ClickHouseTestContainer.DATABASE, reg.value("kudos.test.clickhouse.database"))
    }

    @Test
    fun `clickhouse fails when http port has null ip`() {
        val reg = FakeDynamicPropertyRegistry()
        val c = container(ports = arrayOf(port(null, 48123, privatePort = 8123)))
        val ex = assertFailsWith<IllegalArgumentException> { ClickHouseTestContainer.registerProperties(reg, c) }
        assertEquals("container HTTP port ip is null", ex.message)
    }

    @Test
    fun `clickhouse fails when no 8123 mapping present`() {
        val reg = FakeDynamicPropertyRegistry()
        val c = container(ports = arrayOf(port("ch", 49000, privatePort = 9000)))
        assertFailsWith<NoSuchElementException> { ClickHouseTestContainer.registerProperties(reg, c) }
    }

    @Test
    fun `influxdb registers url token org bucket`() {
        val reg = FakeDynamicPropertyRegistry()
        val c = container(ports = arrayOf(port("idb", 18086, privatePort = 8086)))
        InfluxdbTestContainer.registerProperties(reg, c)
        assertEquals("http://idb:18086", reg.value("kudos.ability.tsdb.influxdb.url"))
        assertEquals(InfluxdbTestContainer.ADMIN_TOKEN, reg.value("kudos.ability.tsdb.influxdb.token"))
        assertEquals(InfluxdbTestContainer.ORG, reg.value("kudos.ability.tsdb.influxdb.org"))
        assertEquals(InfluxdbTestContainer.BUCKET, reg.value("kudos.ability.tsdb.influxdb.bucket"))
    }

    @Test
    fun `influxdb fails when no 8086 mapping`() {
        val reg = FakeDynamicPropertyRegistry()
        val c = container(ports = arrayOf(port("idb", 1, privatePort = 1)))
        val ex = assertFailsWith<IllegalStateException> { InfluxdbTestContainer.registerProperties(reg, c) }
        assertEquals("InfluxDB container exposes no 8086 mapping", ex.message)
    }

    @Test
    fun `influxdb fails when 8086 ip is null`() {
        val reg = FakeDynamicPropertyRegistry()
        val c = container(ports = arrayOf(port(null, 18086, privatePort = 8086)))
        val ex = assertFailsWith<IllegalArgumentException> { InfluxdbTestContainer.registerProperties(reg, c) }
        assertEquals("container port ip is null", ex.message)
    }

    @Test
    fun `nacos registers fixed localhost seata addr for both config and discovery`() {
        val reg = FakeDynamicPropertyRegistry()
        NacosTestContainer.registerProperties(reg, container(ports = arrayOf(port("ignored", 12345))))
        assertEquals("localhost:38848", reg.value("spring.cloud.nacos.config.server-addr"))
        assertEquals("localhost:38848", reg.value("spring.cloud.nacos.discovery.server-addr"))
    }

    @Test
    fun `rocketmq registers fixed name-server addr`() {
        val reg = FakeDynamicPropertyRegistry()
        RocketMqTestContainer.registerProperties(reg, container(ports = arrayOf(port("ignored", 9876))))
        assertEquals(RocketMqTestContainer.NAMESRV_ADDR, reg.value("spring.cloud.stream.rocketmq.binder.name-server"))
        assertTrue(RocketMqTestContainer.NAMESRV_ADDR.endsWith(":${RocketMqTestContainer.PORT}"))
    }

    @Test
    fun `seata registers file registry and grouplist when registry non-null`() {
        val reg = FakeDynamicPropertyRegistry()
        SeataTestContainer.registerProperties(reg, container())
        assertEquals("file", reg.value("seata.registry.type"))
        assertEquals("127.0.0.1:${SeataTestContainer.SERVICE_PORT}", reg.value("seata.service.default.grouplist"))
    }

    @Test
    fun `seata no-op when registry null`() {
        // null registry path returns early without NPE
        SeataTestContainer.registerProperties(null, container())
    }

    @Test
    fun `smtp register is a no-op for both null and non-null registry`() {
        val reg = FakeDynamicPropertyRegistry()
        SmtpTestContainer.registerProperties(reg, container())
        SmtpTestContainer.registerProperties(null, container())
        assertTrue(reg.entries.isEmpty())
    }

    @Test
    fun `wiremock register is a no-op for both null and non-null registry`() {
        val reg = FakeDynamicPropertyRegistry()
        WireMockTestContainer.registerProperties(reg, container())
        WireMockTestContainer.registerProperties(null, container())
        assertTrue(reg.entries.isEmpty())
    }

    @Test
    fun `postgres registers jdbc url for given database`() {
        val reg = FakeDynamicPropertyRegistry()
        PostgresTestContainer.registerProperties(reg, container(ports = arrayOf(port("pg", 15432))), "mydb")
        assertEquals("jdbc:postgresql://pg:15432/mydb", reg.value("spring.datasource.dynamic.datasource.postgres.url"))
        assertEquals(PostgresTestContainer.USERNAME, reg.value("spring.datasource.dynamic.datasource.postgres.username"))
        assertEquals(PostgresTestContainer.PASSWORD, reg.value("spring.datasource.dynamic.datasource.postgres.password"))
    }
}
