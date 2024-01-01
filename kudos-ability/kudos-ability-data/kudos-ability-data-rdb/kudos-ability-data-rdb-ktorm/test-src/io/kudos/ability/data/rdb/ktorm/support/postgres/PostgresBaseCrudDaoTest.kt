package io.kudos.ability.data.rdb.ktorm.support.postgres

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDaoTest
import io.kudos.test.common.EnableKudosTest
import io.kudos.test.common.container.PostgresTestContainer
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container

@EnableKudosTest
@ActiveProfiles("postgres")
internal class PostgresBaseCrudDaoTest : BaseCrudDaoTest() {

    companion object {

        @Container
        @JvmStatic
        val postgresContainer = PostgresTestContainer.CONTAINER

        @DynamicPropertySource
        @JvmStatic
        fun property(registry: DynamicPropertyRegistry) {
            PostgresTestContainer.properties(registry)
        }

    }

}