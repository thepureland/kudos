package io.kudos.ability.data.rdb.ktorm.support.h2

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDaoTest
import io.kudos.test.common.EnableKudosTest
import org.springframework.test.context.ActiveProfiles

@EnableKudosTest
@ActiveProfiles("h2")
internal open class H2BaseCrudDaoTest : BaseCrudDaoTest()