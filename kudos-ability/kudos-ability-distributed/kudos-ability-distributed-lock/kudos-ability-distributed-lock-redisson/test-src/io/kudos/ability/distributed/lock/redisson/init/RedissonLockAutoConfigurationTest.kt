package io.kudos.ability.distributed.lock.redisson.init

import io.kudos.ability.distributed.lock.redisson.init.properties.RedissonBaseConfigProperties
import io.kudos.ability.distributed.lock.redisson.init.properties.RedissonClusterServersProperties
import io.kudos.ability.distributed.lock.redisson.init.properties.RedissonProperties
import io.kudos.ability.distributed.lock.redisson.init.properties.RedissonSingleServerProperties
import io.kudos.ability.distributed.lock.redisson.kit.RedissonLockKit
import org.redisson.config.Config
import org.redisson.config.ReadMode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for [RedissonLockAutoConfiguration] covering: disabled-client short-circuit,
 * single/cluster/unknown-mode config initialization, password handling (null/blank/set),
 * null sub-config tolerance, bean factory methods and the component name.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class RedissonLockAutoConfigurationTest {

    private val configuration = RedissonLockAutoConfiguration()

    @AfterTest
    fun tearDown() {
        RedissonLockKit.setLockKeyPrefix(RedissonLockKit.DEFAULT_LOCK_KEY_PREFIX)
        RedissonLockKit.clearCachedLockers()
    }

    @Test
    fun redissonClient_returnsNullWhenDisabled() {
        val properties = RedissonProperties() // enabled defaults to false
        assertNull(configuration.redissonClient(properties))
    }

    @Test
    fun initRedissonConfig_singleMode_appliesBaseAndSingleServerConfig() {
        val config = Config()
        val properties = RedissonProperties().apply {
            mode = "single"
            baseConfig = RedissonBaseConfigProperties().apply {
                password = "s3cret"
                pingConnectionInterval = 7
                idleConnectionTimeout = 11000
                connectTimeout = 12000
                timeout = 3500
                retryAttempts = 5
                retryInterval = 2000
                subscriptionsPerConnection = 9
                clientName = "unit-test-client"
                subscriptionConnectionMinimumIdleSize = 2
                subscriptionConnectionPoolSize = 51
                dnsMonitoringInterval = 6000L
            }
            singleServerConfig = RedissonSingleServerProperties().apply {
                address = "redis://127.0.0.1:16379"
                connectionMinimumIdleSize = 33
                connectionPoolSize = 65
                database = 3
            }
        }

        configuration.initRedissonConfig(config, properties)

        assertEquals("s3cret", config.password)
        val single = config.useSingleServer()
        assertEquals("redis://127.0.0.1:16379", single.address)
        assertEquals(33, single.connectionMinimumIdleSize)
        assertEquals(65, single.connectionPoolSize)
        assertEquals(3, single.database)
        assertEquals(7, single.pingConnectionInterval)
        assertEquals(11000, single.idleConnectionTimeout)
        assertEquals(12000, single.connectTimeout)
        assertEquals(3500, single.timeout)
        assertEquals(5, single.retryAttempts)
        assertNotNull(single.retryDelay)
        assertEquals(9, single.subscriptionsPerConnection)
        assertEquals("unit-test-client", single.clientName)
        assertEquals(2, single.subscriptionConnectionMinimumIdleSize)
        assertEquals(51, single.subscriptionConnectionPoolSize)
        assertEquals(6000L, single.dnsMonitoringInterval)
    }

    @Test
    fun initRedissonConfig_singleMode_withNullSubConfigsDoesNothing() {
        val config = Config()
        val properties = RedissonProperties().apply {
            mode = "single"
            baseConfig = null
            singleServerConfig = null
        }

        configuration.initRedissonConfig(config, properties)

        // No password and only the (default-valued) single-server config object is created.
        assertNull(config.password)
        assertNull(config.useSingleServer().address)
    }

    @Test
    fun initRedissonConfig_blankPasswordIsNotApplied() {
        val config = Config()
        val properties = RedissonProperties().apply {
            mode = "single"
            baseConfig = RedissonBaseConfigProperties().apply { password = "  " }
        }

        configuration.initRedissonConfig(config, properties)

        assertNull(config.password)
    }

    @Test
    fun initRedissonConfig_clusterMode_appliesBaseAndClusterServersConfig() {
        val config = Config()
        val properties = RedissonProperties().apply {
            mode = "cluster"
            baseConfig = RedissonBaseConfigProperties().apply {
                password = "cluster-pwd"
                subscriptionConnectionMinimumIdleSize = 4
                subscriptionConnectionPoolSize = 52
                dnsMonitoringInterval = 7000L
                clientName = "cluster-client"
                retryAttempts = 2
            }
            clusterServersConfig = RedissonClusterServersProperties().apply {
                slaveConnectionMinimumIdleSize = 16
                slaveConnectionPoolSize = 48
                masterConnectionMinimumIdleSize = 17
                masterConnectionPoolSize = 49
                readMode = "MASTER"
                scanInterval = 2500
                nodeAddresses = arrayOf("redis://10.0.0.1:7000", "redis://10.0.0.2:7001")
            }
        }

        configuration.initRedissonConfig(config, properties)

        assertEquals("cluster-pwd", config.password)
        val cluster = config.useClusterServers()
        assertEquals(16, cluster.slaveConnectionMinimumIdleSize)
        assertEquals(48, cluster.slaveConnectionPoolSize)
        assertEquals(17, cluster.masterConnectionMinimumIdleSize)
        assertEquals(49, cluster.masterConnectionPoolSize)
        assertEquals(ReadMode.MASTER, cluster.readMode)
        assertEquals(2500, cluster.scanInterval)
        assertEquals(listOf("redis://10.0.0.1:7000", "redis://10.0.0.2:7001"), cluster.nodeAddresses)
        assertEquals(4, cluster.subscriptionConnectionMinimumIdleSize)
        assertEquals(52, cluster.subscriptionConnectionPoolSize)
        assertEquals(7000L, cluster.dnsMonitoringInterval)
        assertEquals("cluster-client", cluster.clientName)
        assertEquals(2, cluster.retryAttempts)
    }

    @Test
    fun initRedissonConfig_clusterMode_withNullSubConfigsDoesNothing() {
        val config = Config()
        val properties = RedissonProperties().apply {
            mode = "cluster"
            baseConfig = null
            clusterServersConfig = null
        }

        configuration.initRedissonConfig(config, properties)

        assertNull(config.password)
        assertEquals(emptyList(), config.useClusterServers().nodeAddresses)
    }

    @Test
    fun initRedissonConfig_unknownModeLeavesConfigUntouched() {
        val config = Config()
        val properties = RedissonProperties().apply {
            mode = "sentinel"
            baseConfig = RedissonBaseConfigProperties().apply { password = "p" }
        }

        configuration.initRedissonConfig(config, properties)

        // Password set at the top, but neither single nor cluster config is initialized.
        assertEquals("p", config.password)
    }

    @Test
    fun beanFactoryMethods_returnInstances() {
        assertNotNull(configuration.redissonLockProvider())
        assertNotNull(configuration.distributedLockAspect())
        assertNotNull(configuration.redissonProperties())
    }

    @Test
    fun redissonLocker_appliesLockKeyPrefixFromProperties() {
        val properties = RedissonProperties().apply { lockKeyPrefix = "UNIT::" }

        val locker = configuration.redissonLocker(properties)

        assertNotNull(locker)
        assertEquals("UNIT::myKey", RedissonLockKit.getLockKey("myKey"))
    }

    @Test
    fun getComponentName_returnsModuleName() {
        assertEquals("kudos-ability-distributed-lock-redisson", configuration.getComponentName())
    }
}
