package io.kudos.context.config

import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertySource

/**
 * Test-only [IConfigDataFinder] registered via META-INF/services (ServiceLoader).
 *
 * It only reacts to source names with the dedicated "yaml-test-" prefix used by
 * [YamlPropertySourceFactoryTest], so its presence on the test classpath cannot leak into
 * any other test (every other lookup gets null, i.e. "no config center data").
 *
 * @author K
 * @since 1.0.0
 */
class TestConfigDataFinder : IConfigDataFinder {

    override fun findConfigData(name: String?): PropertySource<*>? = when (name) {
        // A mutable-map-backed source: triggers the "merge config center over local yaml" branch
        "yaml-test-map" -> MapPropertySource(
            "yaml-test-map",
            linkedMapOf<String, Any>(
                "kudos.center.only" to "from-center",
                "kudos.conflict" to "center-wins"
            )
        )
        // A non-map source: triggers the "return the config center property source directly" branch
        "yaml-test-raw" -> object : PropertySource<String>("yaml-test-raw", "raw-source") {
            override fun getProperty(name: String): Any? = if (name == "raw.key") "raw-value" else null
        }
        else -> null
    }
}
