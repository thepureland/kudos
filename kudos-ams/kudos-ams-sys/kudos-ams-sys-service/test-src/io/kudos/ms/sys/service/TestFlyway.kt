package io.kudos.ms.sys.service

import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.containers.H2TestContainer
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable
import kotlin.test.Test

@EnableKudosTest
@EnabledIfDockerAvailable
//@Import(FlywayAutoConfiguration::class)
class TestFlyway {

    @Test
    fun test(){
        println("test")
//        println("Applied migrations: " + flyway.info().appliedMigrations.map { it.version.toString() })
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        private fun changeProperties(registry: DynamicPropertyRegistry) {
            H2TestContainer.startIfNeeded(registry)
        }
    }

}