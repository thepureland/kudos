package io.kudos.ability.log.audit.common.support

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

// (DefaultImpls bridge test below uses reflection — no direct import possible for a synthetic class)

/**
 * Unit tests for [DefaultAuditLogDetailDescriptionFormatter] and the
 * [IAuditLogDetailDescriptionFormatter] interface defaults.
 *
 * Coverage:
 *  - [DefaultAuditLogDetailDescriptionFormatter.descriptionFormat] always returns "" — "do not generate
 *    a description" semantics, including the null-input edge.
 *  - Interface defaults inherited untouched: [IAuditLogDetailDescriptionFormatter.needFormat] is false
 *    (the default formatter never opts into the auto-pick path) and
 *    [IAuditLogDetailDescriptionFormatter.loadOldBizData] yields null (no old-data diff by default).
 *
 * @author K
 * @since 1.0.0
 */
internal class DefaultAuditLogDetailDescriptionFormatterTest {

    private val formatter = DefaultAuditLogDetailDescriptionFormatter()

    @Test
    fun descriptionFormat_alwaysEmpty_evenForNullInput() {
        assertEquals("", formatter.descriptionFormat(null))
    }

    @Test
    fun needFormat_defaultsToFalse() {
        // Critical: if the default formatter ever returned true it would always win the auto-pick
        // scan in AuditLogTool.descriptionFormatter and shadow every business formatter.
        assertFalse(formatter.needFormat(null))
    }

    @Test
    fun loadOldBizData_defaultsToNull() {
        assertNull(formatter.loadOldBizData(null, null))
        assertNull(formatter.loadOldBizData(null, arrayOf("arg-1", null)))
    }

    @Test
    fun defaultImplsCompatibilityBridges_matchInterfaceDefaults() {
        // Kotlin also compiles interface defaults into a synthetic `DefaultImpls` holder for
        // compatibility-mode implementors (e.g. Java classes / older Kotlin clients). Exercise that
        // entry point too so both compiled routes are guaranteed to share the same semantics.
        val defaultImpls = Class.forName(
            "io.kudos.ability.log.audit.common.support.IAuditLogDetailDescriptionFormatter\$DefaultImpls"
        )
        val needFormat = defaultImpls.getMethod(
            "needFormat", IAuditLogDetailDescriptionFormatter::class.java, io.kudos.ability.log.audit.common.entity.BaseLog::class.java
        )
        assertEquals(false, needFormat.invoke(null, formatter, null))

        val loadOldBizData = defaultImpls.getMethod(
            "loadOldBizData",
            IAuditLogDetailDescriptionFormatter::class.java,
            io.kudos.ability.log.audit.common.entity.BaseLog::class.java,
            Array<Any?>::class.java,
        )
        assertNull(loadOldBizData.invoke(null, formatter, null, null))
    }
}
