package io.kudos.test.container.containers

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Sanity-checks the public constants and computed values of each container object: image names,
 * labels, ports, and derived addresses/tokens. Touching these forces class initialization of every
 * container object (covering field initializers that need no Docker daemon).
 *
 * @author K
 * @since 1.0.0
 */
internal class ContainerConstantsTest {

    @Test
    fun `redis constants`() {
        assertEquals("redis:8.6.0-alpine", RedisTestContainer.IMAGE_NAME_REDIS)
        assertEquals("Redis", RedisTestContainer.LABEL)
    }

    @Test
    fun `h2 constants and default image`() {
        assertTrue(H2TestContainer.DATABASE.startsWith("test_")) // per-JVM-unique name, e.g. test_<uuid>
        assertEquals(1521, H2TestContainer.PORT)
        assertEquals("sa", H2TestContainer.USERNAME)
        assertEquals("sa", H2TestContainer.PASSWORD)
        assertEquals("H2", H2TestContainer.LABEL)
    }

    @Test
    fun `kafka label`() {
        assertEquals("Kafka", KafkaTestContainer.LABEL)
    }

    @Test
    fun `mysql constants`() {
        assertEquals(23306, MySqlTestContainer.PORT)
        assertEquals(3306, MySqlTestContainer.CONTAINER_PORT)
        assertEquals("test", MySqlTestContainer.DATABASE)
        assertEquals("root", MySqlTestContainer.USERNAME)
        assertEquals("mysql", MySqlTestContainer.PASSWORD)
        assertEquals("MySql", MySqlTestContainer.LABEL)
    }

    @Test
    fun `mongo constants`() {
        assertEquals("mongo:7.0.20", MongoTestContainer.IMAGE_NAME_MONGO)
        assertEquals("Mongo", MongoTestContainer.LABEL)
    }

    @Test
    fun `rabbitmq constants`() {
        assertEquals("rabbitmq:4.1.4-alpine", RabbitMqTestContainer.IMAGE_NAME)
        assertEquals(25672, RabbitMqTestContainer.PORT)
        assertEquals(5672, RabbitMqTestContainer.CONTAINER_PORT)
        assertEquals("RabbitMQ", RabbitMqTestContainer.LABEL)
    }

    @Test
    fun `minio constants`() {
        assertEquals(9000, MinioTestContainer.PORT)
        assertEquals("Minio", MinioTestContainer.LABEL)
        assertTrue(MinioTestContainer.IMAGE_NAME.startsWith("minio/minio:"))
    }

    @Test
    fun `clickhouse constants`() {
        assertEquals("clickhouse/clickhouse-server:24.8-alpine", ClickHouseTestContainer.IMAGE_NAME_CLICKHOUSE)
        assertEquals("ClickHouse", ClickHouseTestContainer.LABEL)
        assertEquals("kudos_test", ClickHouseTestContainer.DATABASE)
        assertEquals("default", ClickHouseTestContainer.USERNAME)
        assertEquals("", ClickHouseTestContainer.PASSWORD)
    }

    @Test
    fun `influxdb constants and 32-byte token`() {
        assertEquals("influxdb:2.7-alpine", InfluxdbTestContainer.IMAGE_NAME_INFLUXDB)
        assertEquals("InfluxDB", InfluxdbTestContainer.LABEL)
        assertEquals("kudos-test-org", InfluxdbTestContainer.ORG)
        assertEquals("kudos-test-bucket", InfluxdbTestContainer.BUCKET)
        assertEquals(32, InfluxdbTestContainer.ADMIN_TOKEN.length)
    }

    @Test
    fun `nacos constants and base64 token round-trips`() {
        assertEquals(28848, NacosTestContainer.PORT)
        assertEquals("Nacos", NacosTestContainer.LABEL)
        assertEquals("Nacos (for Seata)", NacosTestContainer.LABEL_NACOS_FOR_SEATA)
        assertEquals(32, NacosTestContainer.tokenBytes.size)
        val decoded = Base64.getDecoder().decode(NacosTestContainer.tokenBase64)
        assertContentEquals(NacosTestContainer.tokenBytes, decoded)
    }

    @Test
    fun `rocketmq constants and derived namesrv addr`() {
        assertEquals(9876, RocketMqTestContainer.PORT)
        assertEquals("RocketMQ name server", RocketMqTestContainer.LABEL_NANE_SERVER)
        assertEquals("RocketMQ broker server", RocketMqTestContainer.LABEL_BROKER_SERVER)
        assertTrue(RocketMqTestContainer.NAMESRV_ADDR.contains(":9876"))
    }

    @Test
    fun `seata constants`() {
        assertEquals(28091, SeataTestContainer.SERVICE_PORT)
        assertEquals("Seata", SeataTestContainer.LABEL)
    }

    @Test
    fun `smtp constants`() {
        assertEquals("namshi/smtp:latest", SmtpTestContainer.IMAGE_NAME)
        assertEquals("Smtp", SmtpTestContainer.LABEL)
    }

    @Test
    fun `wiremock label`() {
        assertEquals("WireMock", WireMockTestContainer.LABEL)
    }

    @Test
    fun `postgres constants`() {
        assertEquals("test", PostgresTestContainer.DATABASE)
        assertEquals(5432, PostgresTestContainer.CONTAINER_PORT)
        assertEquals("pg", PostgresTestContainer.USERNAME)
        assertEquals("postgres", PostgresTestContainer.PASSWORD)
        assertEquals("PostgreSql", PostgresTestContainer.LABEL)
        assertEquals("kudos.test.postgres.data.dir", PostgresTestContainer.SYS_PROP_HOST_DATA_DIR)
        assertEquals("KUDOS_TEST_POSTGRES_DATA_DIR", PostgresTestContainer.ENV_HOST_DATA_DIR)
    }
}
