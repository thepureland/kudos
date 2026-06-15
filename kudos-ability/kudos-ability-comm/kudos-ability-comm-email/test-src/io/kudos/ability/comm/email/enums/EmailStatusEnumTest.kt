package io.kudos.ability.comm.email.enums

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * test for EmailStatusEnum
 *
 * 覆蓋：全部枚舉項的 code 與 displayText、枚舉項數量與順序（業務側可能依賴 code 持久化）。
 *
 * @author K
 * @since 1.0.0
 */
internal class EmailStatusEnumTest {

    @Test
    fun codes() {
        assertEquals("0", EmailStatusEnum.FAIL.code)
        assertEquals("1", EmailStatusEnum.SUCCESS_PART.code)
        assertEquals("2", EmailStatusEnum.SUCCESS.code)
    }

    @Test
    fun displayTexts() {
        assertEquals("Failed", EmailStatusEnum.FAIL.displayText)
        assertEquals("Partial success", EmailStatusEnum.SUCCESS_PART.displayText)
        assertEquals("Success", EmailStatusEnum.SUCCESS.displayText)
    }

    @Test
    fun entriesStable() {
        // code 會被業務側持久化，枚舉項數量與順序不可隨意變動
        assertEquals(3, EmailStatusEnum.entries.size)
        assertEquals(
            listOf(EmailStatusEnum.FAIL, EmailStatusEnum.SUCCESS_PART, EmailStatusEnum.SUCCESS),
            EmailStatusEnum.entries.toList()
        )
    }

}
