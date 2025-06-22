package io.kudos.ability.data.rdb.ktorm.metadata

import io.kudos.ability.data.rdb.jdbc.metadata.Column
import io.kudos.ability.data.rdb.ktorm.support.KtormSqlType


/**
 * 返回Ktorm框架对应的sql类型的函数名
 */
fun Column.getKtormSqlTypeFunName(): String = KtormSqlType.getFunName(kotlinType)