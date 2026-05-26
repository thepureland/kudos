package io.kudos.ability.log.audit.common.entity

import io.kudos.ability.log.audit.common.annotation.Audit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.log.audit.common.support.ISysAuditModule
import io.kudos.context.kit.SpringKit
import org.springframework.context.support.StaticApplicationContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for chained resolution across multiple [ISysAuditModule] implementations in [BaseLog.initModule].
 *
 * Verifies the round-2 fix: from "use only `values.first()`" to "iterate each; the first to return
 * a non-null id / name wins". Coverage:
 *  - Single implementation, direct hit.
 *  - Multiple impls: the first returns null/null and the second produces results — the second should be used.
 *  - Multiple impls: id from one, name from another (accumulated independently).
 *  - All return null — id/name remain null (no NPE).
 *  - No ISysAuditModule implementation registered — id/name are not written.
 *
 * The legacy `init` block + `SpringKit.getBean()` did not retry when SpringKit was not yet ready;
 * this test also serves as an indirect guard for the round-2 fix in
 * [io.kudos.ability.log.audit.common.support.AuditLogTool].
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class BaseLogInitModuleTest {

    private lateinit var ctx: StaticApplicationContext

    @BeforeTest
    fun setup() {
        ctx = StaticApplicationContext().apply { refresh() }
        SpringKit.applicationContext = ctx
    }

    @AfterTest
    fun teardown() {
        ctx.close()
    }

    @Test
    fun singleModule_resolvesIdAndName() {
        registerModule("only", id = 42, name = "User")
        val log = baseLog(moduleCode = "USER")
        assertEquals(42, log.moduleId)
        assertEquals("User", log.moduleName)
    }

    @Test
    fun multiModules_firstReturnsNull_secondHits() {
        registerModule("first", id = null, name = null)
        registerModule("second", id = 7, name = "Order")
        val log = baseLog(moduleCode = "ORDER")
        assertEquals(7, log.moduleId)
        assertEquals("Order", log.moduleName)
    }

    @Test
    fun multiModules_idFromOneNameFromAnother() {
        // One knows only id, the other only name — the chained merge stitches the halves together
        registerModule("first", id = 99, name = null)
        registerModule("second", id = null, name = "Stock")
        val log = baseLog(moduleCode = "STOCK")
        assertEquals(99, log.moduleId)
        assertEquals("Stock", log.moduleName)
    }

    @Test
    fun allReturnNull_keepsModuleNullable() {
        registerModule("first", id = null, name = null)
        registerModule("second", id = null, name = null)
        val log = baseLog(moduleCode = "UNKNOWN")
        assertNull(log.moduleId)
        assertNull(log.moduleName)
    }

    @Test
    fun noModulesRegistered_keepsModuleNullable() {
        // Don't register any ISysAuditModule — initModule should return immediately
        val log = baseLog(moduleCode = "ANY")
        assertNull(log.moduleId)
        assertNull(log.moduleName)
    }

    private fun registerModule(beanName: String, id: Int?, name: String?) {
        ctx.beanFactory.registerSingleton(beanName, object : ISysAuditModule {
            override fun module(subsysCode: String?, moduleCode: String?): Pair<Int?, String?> = id to name
        })
    }

    private fun baseLog(moduleCode: String): BaseLog {
        // Kotlin annotation classes can be invoked as constructors directly — much cleaner than reflective construction + injection
        val annotation = Audit(
            opType = OperationTypeEnum.CREATE,
            moduleCode = moduleCode,
        )
        return BaseLog(annotation)
    }
}
