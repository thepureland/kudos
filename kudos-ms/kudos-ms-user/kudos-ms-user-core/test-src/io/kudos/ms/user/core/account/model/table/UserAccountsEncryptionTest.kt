package io.kudos.ms.user.core.account.model.table

import io.kudos.ability.data.rdb.ktorm.support.EncryptedVarcharSqlType
import kotlin.test.Test
import kotlin.test.assertIs

internal class UserAccountsEncryptionTest {

    @Test
    fun authenticationKeyUsesTransparentAuthenticatedEncryption() {
        assertIs<EncryptedVarcharSqlType>(UserAccounts.authenticationKey.sqlType)
    }
}
