package io.kudos.ability.data.rdb.ktorm.support.mysql

import io.kudos.ability.data.rdb.ktorm.support.BaseReadOnlyDaoTest
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.MySqlTestContainer
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource


@EnableKudosTest
@ActiveProfiles("mysql")
internal class MySqlBaseReadOnlyDaoTest : BaseReadOnlyDaoTest() {
    companion object {

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            MySqlTestContainer.start(registry)
        }

    }

}