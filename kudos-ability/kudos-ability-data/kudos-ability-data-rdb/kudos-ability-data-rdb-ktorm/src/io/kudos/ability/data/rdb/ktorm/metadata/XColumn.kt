package io.kudos.ability.data.rdb.ktorm.metadata

import io.kudos.ability.data.rdb.jdbc.metadata.Column
import io.kudos.ability.data.rdb.ktorm.support.KtormSqlType


/**
 * Returns the function name of the SQL type corresponding to the Ktorm framework.
 */
fun Column.getKtormSqlTypeFunName(): String = KtormSqlType.getFunName(kotlinType)