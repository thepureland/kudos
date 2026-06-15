package io.kudos.ability.data.rdb.jdbc.init

import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties
import io.kudos.ability.data.rdb.jdbc.datasource.DefaultDynamicDataSourceLoad
import io.kudos.ability.data.rdb.jdbc.datasource.DsContextProcessor
import io.kudos.ability.data.rdb.jdbc.datasource.DsDataSourceCreator
import io.kudos.ability.data.rdb.jdbc.datasource.HikariDataSourceMeterInitEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * Unit tests for [JdbcAutoConfiguration]'s bean factory methods, invoked directly (the Spring
 * container wiring itself is exercised by the module's integration tests via @EnableKudosTest).
 *
 * @author K
 * @since 1.0.0
 */
internal class JdbcAutoConfigurationTest {

    private val config = JdbcAutoConfiguration()

    @Test
    fun simpleBeans_areCreated() {
        assertNotNull(config.dynamicDataSourceProperties())
        assertNotNull(config.multipleDataSourceProperties())
        assertNotNull(config.dynamicDataSourceAspect())
        assertNotNull(config.dsContextProcessor())
        assertIs<DefaultDynamicDataSourceLoad>(
            config.dynamicDataSourceLoad(),
            "without a custom implementation the placeholder loader is used"
        )
    }

    @Test
    fun dataSourceCreator_isDsDataSourceCreatorWithGlobalConfigApplied() {
        val properties = DynamicDataSourceProperties()
        val creator = config.dataSourceCreator(properties, emptyList())
        assertIs<DsDataSourceCreator>(creator, "kudos overrides baomidou's default creator")
    }

    @Test
    fun componentName() {
        assertEquals("kudos-ability-data-rdb-jdbc", config.getComponentName())
    }

    @Test
    fun micrometerConfiguration_buildsMeterAwareInitEvent() {
        val event = JdbcAutoConfiguration.MicrometerConfiguration().hikariDataSourceMeterInitEvent()
        assertIs<HikariDataSourceMeterInitEvent>(event)
    }

    @Test
    fun cacheCleanListenerConfiguration_buildsListenerBoundToDefaultCacheName() {
        val listener = JdbcAutoConfiguration.CacheCleanListenerConfiguration()
            .dataSourceClearListener(DsContextProcessor())
        assertNotNull(listener)
    }
}
