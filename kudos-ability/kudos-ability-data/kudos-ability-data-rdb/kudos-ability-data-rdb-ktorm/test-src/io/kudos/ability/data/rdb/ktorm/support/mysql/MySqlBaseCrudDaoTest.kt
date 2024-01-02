package io.kudos.ability.data.rdb.ktorm.support.mysql

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDaoTest
import io.kudos.test.common.EnableKudosTest
import io.kudos.test.common.container.MySqlTestContainer
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container

@EnableKudosTest
@ActiveProfiles("mysql")
internal class MySqlBaseCrudDaoTest : BaseCrudDaoTest() {

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