package io.kudos.ability.data.rdb.ktorm.metadata

import io.kudos.ability.data.rdb.jdbc.metadata.Column
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for the [Column.getKtormSqlTypeFunName] extension: it must delegate to
 * [io.kudos.ability.data.rdb.ktorm.support.KtormSqlType] based on the column's kotlinType.
 * Exhaustive type mapping is covered by KtormSqlTypeTest; here we lock the delegation itself.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class XColumnTest {

    @Test
    fun getKtormSqlTypeFunName_delegatesToKtormSqlTypeMapping() {
        val column = Column().apply { kotlinType = String::class }
        assertEquals("varchar", column.getKtormSqlTypeFunName())

        column.kotlinType = Int::class
        assertEquals("int", column.getKtormSqlTypeFunName())

        column.kotlinType = java.time.LocalDateTime::class
        assertEquals("datetime", column.getKtormSqlTypeFunName())

        // unknown type falls through to the empty-string default
        column.kotlinType = Any::class
        assertEquals("", column.getKtormSqlTypeFunName())
    }
}
