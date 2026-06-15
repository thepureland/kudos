package io.kudos.contexttest.fixture

import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.context.annotation.Configuration

/**
 * Fixtures for ComponentInitializationDispatcher / ComponentInitializerSelector tests.
 *
 * Deliberately placed OUTSIDE the io.kudos.context / io.kudos.ability / io.kudos.ms roots
 * scanned by ComponentInitializerSelector, so these classes are never auto-imported into the
 * Spring Boot test context of other test classes (@EnableKudosTest) running in the same JVM.
 *
 * @author K
 * @since 1.0.0
 */

/** Initializer that records how many times its lifecycle callbacks fire. */
class RecordingInitializer : IComponentInitializer {
    var beforeCount = 0
        private set
    var afterCount = 0
        private set

    override fun getComponentName() = "recording"
    override fun beforeInit() {
        beforeCount++
    }

    override fun afterInit() {
        afterCount++
    }
}

/** Initializer that counts how often the default lifecycle logging queries the component name. */
class CountingNameInitializer : IComponentInitializer {
    var nameQueries = 0
        private set

    override fun getComponentName(): String {
        nameQueries++
        return "test-component"
    }
}

/** @Configuration initializer that must start after [CfgB]. */
@Configuration
@AutoConfigureAfter(CfgB::class)
open class CfgA : IComponentInitializer {
    override fun getComponentName() = "cfgA"
}

/** Plain @Configuration initializer, the dependency target of [CfgA] and [CfgC]. */
@Configuration
open class CfgB : IComponentInitializer {
    override fun getComponentName() = "cfgB"
}

/** @Configuration initializer that must start before [CfgB] (reverse dependency). */
@Configuration
@AutoConfigureBefore(CfgB::class)
open class CfgC : IComponentInitializer {
    override fun getComponentName() = "cfgC"
}

/** @AutoConfigureAfter pointing at a class that is NOT registered as a candidate (the empty-dependsOn branch). */
@Configuration
@AutoConfigureAfter(UnregisteredCfg::class)
open class CfgE : IComponentInitializer {
    override fun getComponentName() = "cfgE"
}

/** Never registered in the test registry. */
open class UnregisteredCfg

/** @Configuration but NOT an IComponentInitializer -> must not become a candidate. */
@Configuration
open class CfgNotInitializer

/** IComponentInitializer but NOT @Configuration -> must not become a candidate. */
open class PlainInitializer : IComponentInitializer {
    override fun getComponentName() = "plain"
}
