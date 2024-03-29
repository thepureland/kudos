package io.kudos.ability.data.rdb.ktorm.support.sqlite

import io.kudos.ability.data.rdb.ktorm.support.BaseReadOnlyDaoTest
import io.kudos.test.common.EnableKudosTest
import org.springframework.test.context.ActiveProfiles


@EnableKudosTest
@ActiveProfiles("sqlite")
internal class SQLiteBaseReadOnlyDaoTest : BaseReadOnlyDaoTest()