package io.kudos.ability.data.rdb.ktorm.table

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import org.springframework.stereotype.Repository

/**
 * DAO for the auto-increment test table.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Repository
internal open class TestAutoIncKtormDao : BaseCrudDao<Int, TestAutoIncKtorm, TestAutoIncKtorms>()
