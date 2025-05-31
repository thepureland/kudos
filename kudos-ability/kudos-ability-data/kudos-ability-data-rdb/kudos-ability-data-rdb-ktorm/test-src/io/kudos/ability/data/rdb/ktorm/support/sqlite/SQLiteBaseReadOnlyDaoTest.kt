package io.kudos.ability.data.rdb.ktorm.support.sqlite

import io.kudos.ability.data.rdb.ktorm.support.BaseReadOnlyDaoTest
import io.kudos.test.common.init.EnableKudosTest
import org.junit.jupiter.api.Disabled
import org.springframework.test.context.ActiveProfiles


@EnableKudosTest
@ActiveProfiles("sqlite")
@Disabled
internal class SQLiteBaseReadOnlyDaoTest : BaseReadOnlyDaoTest()