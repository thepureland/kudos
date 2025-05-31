package io.kudos.ability.data.rdb.ktorm.support.postgres

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDaoTest
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.PostgresTestContainer
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@EnableKudosTest
@ActiveProfiles("postgres")
internal class PostgresBaseCrudDaoTest : BaseCrudDaoTest() {

    companion object {

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            PostgresTestContainer.start(registry)
        }

    }

}