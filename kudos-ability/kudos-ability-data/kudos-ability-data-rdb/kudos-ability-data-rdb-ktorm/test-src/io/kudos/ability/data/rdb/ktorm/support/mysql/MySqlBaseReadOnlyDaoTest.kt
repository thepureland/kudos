package io.kudos.ability.data.rdb.ktorm.support.mysql

import io.kudos.ability.data.rdb.ktorm.support.BaseReadOnlyDaoTest
import io.kudos.test.common.EnableKudosTest
import io.kudos.test.common.container.MySqlTestContainer
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers


@EnableKudosTest
@ActiveProfiles("mysql")
internal class MySqlBaseReadOnlyDaoTest : BaseReadOnlyDaoTest() {
    companion object {

        @Container
        @JvmStatic
        val mysqlContainer = MySqlTestContainer.CONTAINER

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            MySqlTestContainer.start(registry)
        }

    }

}