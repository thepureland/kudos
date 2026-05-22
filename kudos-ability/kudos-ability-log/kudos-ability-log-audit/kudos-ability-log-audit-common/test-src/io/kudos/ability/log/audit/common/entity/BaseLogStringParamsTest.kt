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
 * [BaseLog.getStringParams] / [BaseLog.splitStringParams] 的 round-trip 单测。
 *
 * 覆盖：
 *  - 普通参数（不含 `┼`）拼接 + 解析回原值
 *  - 参数自身含 `┼` → 转义后拼接 + 解析回原值（修复历史 bug：旧实现反向解析会错位）
 *  - 参数含 `\` → 转义后拼接 + 解析回原值
 *  - 空字符串 / null / 单段 / 边界场景
 *
 * `BaseLog(Audit)` 构造时会查 Spring 里的 `ISysAuditModule` bean——这里用空的
 * [StaticApplicationContext] 喂给 [SpringKit] 让构造路径不抛 "applicationContext not initialized"。
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
        // 中间的 ┼ 应当被转义为 \┼，外层 ┼ 仍然作为分隔符
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
        // 反斜杠应当被转义成 \\，避免与 \┼ 混淆
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
        // 输入: "a\┼b" + "c" —— 反斜杠和 ┼ 都在第一个 segment 里，转义须能区分
        val log = newBaseLog().apply {
            addParam("""a\┼b""")
            addParam("c")
        }
        val joined = log.getStringParams()
        // a\┼b 中：\ → \\，┼ → \┼，所以 segment 输出 a\\\┼b，全字符串 a\\\┼b┼c
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
        // 旧版本数据：参数自身不含 ┼ 时，转义版与旧版结果一致——历史 stringParams 字段可正常解析回
        val legacy = "alice┼admin┼create"
        assertEquals(listOf("alice", "admin", "create"), BaseLog.splitStringParams(legacy))
    }

    private fun newBaseLog(): BaseLog =
        BaseLog(Audit(opType = OperationTypeEnum.CREATE, moduleCode = "TEST"))
}
