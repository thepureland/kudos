package io.kudos.ability.data.rdb.ktorm.table

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import org.springframework.stereotype.Repository

/**
 * DAO for the managed test table (audit auto-fill tests).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Repository
internal open class TestManagedKtormDao : BaseCrudDao<String, TestManagedKtorm, TestManagedKtorms>()
