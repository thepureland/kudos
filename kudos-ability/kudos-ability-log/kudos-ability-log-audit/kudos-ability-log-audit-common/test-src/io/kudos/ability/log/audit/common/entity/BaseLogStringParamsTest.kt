package io.kudos.ability.log.audit.common.entity

import io.kudos.ability.log.audit.common.annotation.Audit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.context.kit.SpringKit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.context.support.StaticApplicationContext
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Round-trip unit tests for [BaseLog.getStringParams] / [BaseLog.splitStringParams].
 *
 * Coverage:
 *  - Plain params (no `┼`): join + parse back to original.
 *  - Params containing `┼`: escape + join + parse back (fixes a historic bug where the old
 *    implementation misaligned during parsing).
 *  - Params containing `\`: escape + join + parse back.
 *  - Empty string / null / single segment / edge cases.
 *
 * `BaseLog(Audit)` looks up the `ISysAuditModule` bean from Spring during construction — an empty
 * [StaticApplicationContext] is supplied to [SpringKit] so the constructor does not throw
 * "applicationContext not initialized".
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BaseLogStringParamsTest {

    private lateinit var ctx: StaticApplicationContext

    @BeforeAll
    fun setup() {
        ctx = StaticApplicationContext().apply { refresh() }
        SpringKit.applicationContext = ctx
    }

    @AfterAll
    fun teardown() {
        ctx.close()
    }

    @Test
    fun roundTrip_plainParams_noSeparatorChar() {
        val log = newBaseLog().apply {
            addParam("alice")
            addParam("admin")
            addParam("create")
        }

        val joined = log.getStringParams()
        assertEquals("alice┼admin┼create", joined)
        assertEquals(listOf("alice", "admin", "create"), BaseLog.splitStringParams(joined))
    }

    @Test
    fun roundTrip_paramContainsSeparator_escapes() {
        val log = newBaseLog().apply {
            addParam("path┼with┼sep")
            addParam("other")
        }

        val joined = log.getStringParams()
        // Inner ┼ should be escaped to \┼ while the outer ┼ remains the separator
        assertEquals("""path\┼with\┼sep┼other""", joined)
        assertEquals(listOf("path┼with┼sep", "other"), BaseLog.splitStringParams(joined))
    }

    @Test
    fun roundTrip_paramContainsBackslash_escapes() {
        val log = newBaseLog().apply {
            addParam("""C:\Users\test""")
            addParam("x")
        }

        val joined = log.getStringParams()
        // Backslashes should be escaped to \\ to avoid confusion with \┼
        assertEquals("""C:\\Users\\test┼x""", joined)
        assertEquals(listOf("""C:\Users\test""", "x"), BaseLog.splitStringParams(joined))
    }

    @Test
    fun roundTrip_singleParam_noSeparator() {
        val log = newBaseLog().apply { addParam("lone") }
        val joined = log.getStringParams()
        assertEquals("lone", joined)
        assertEquals(listOf("lone"), BaseLog.splitStringParams(joined))
    }

    @Test
    fun roundTrip_paramContainsBackslashFollowedBySeparator() {
        // Input: "a\┼b" + "c" — backslash and ┼ are both in the first segment, escaping must distinguish them
        val log = newBaseLog().apply {
            addParam("""a\┼b""")
            addParam("c")
        }
        val joined = log.getStringParams()
        // In a\┼b: \ -> \\, ┼ -> \┼, so the segment becomes a\\\┼b, full string a\\\┼b┼c
        assertEquals("""a\\\┼b┼c""", joined)
        assertEquals(listOf("""a\┼b""", "c"), BaseLog.splitStringParams(joined))
    }

    @Test
    fun getStringParams_emptyList_returnsNull() {
        val log = newBaseLog()
        assertEquals(null, log.getStringParams())
    }

    @Test
    fun split_emptyInput_returnsEmptyList() {
        assertEquals(emptyList(), BaseLog.splitStringParams(""))
        assertEquals(emptyList(), BaseLog.splitStringParams(null))
    }

    @Test
    fun split_legacyDataWithoutEscape_compatibleWhenNoSeparatorInContent() {
        // Legacy data: when params contain no ┼, the escaped and legacy outputs are identical — historic stringParams values still parse back correctly
        val legacy = "alice┼admin┼create"
        assertEquals(listOf("alice", "admin", "create"), BaseLog.splitStringParams(legacy))
    }

    private fun newBaseLog(): BaseLog =
        BaseLog(Audit(opType = OperationTypeEnum.CREATE, moduleCode = "TEST"))
}
