package io.kudos.ability.data.rdb.ktorm.support.h2

import io.kudos.ability.data.rdb.ktorm.support.BaseReadOnlyDaoTest
import io.kudos.test.common.init.EnableKudosTest
import org.springframework.test.context.ActiveProfiles


@EnableKudosTest
@ActiveProfiles("h2")
internal open class H2BaseReadOnlyDaoTest : BaseReadOnlyDaoTest()