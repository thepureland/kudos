package io.kudos.ability.data.rdb.ktorm.metadata

import io.kudos.ability.data.rdb.ktorm.support.KtormSqlType
import org.soul.ability.data.rdb.jdbc.metadata.Column
import kotlin.reflect.KClass


var Column.kotlinType: KClass<*>
    get() = this.kotlinType
    set(value) {
        this.kotlinType = value
    }


/**
 * 返回Ktorm框架对应的sql类型的函数名
 */
fun Column.getKtormSqlTypeFunName(): String = KtormSqlType.getFunName(kotlinType)