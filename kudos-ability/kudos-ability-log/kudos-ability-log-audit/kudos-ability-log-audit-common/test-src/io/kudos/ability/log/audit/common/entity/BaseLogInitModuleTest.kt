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
 * [BaseLog.initModule] 多 [ISysAuditModule] 链式解析单测。
 *
 * 验证 round-2 修复：从"`values.first()` 只用第一个"改为"遍历每个，第一个返回非空 id /
 * name 的胜出"。覆盖：
 *  - 单实现命中
 *  - 多实现：第一个返回 null/null，第二个有结果——应取第二个
 *  - 多实现：id 来自一个，name 来自另一个（独立累计）
 *  - 全部返回 null——id/name 仍为 null（不爆 NPE）
 *  - 没有任何 ISysAuditModule 实现——不写 id/name
 *
 * 旧的 `init` 块 + `SpringKit.getBean()` 在 SpringKit 未就绪时无重试——本测试也作为
 * round-2 [io.kudos.ability.log.audit.common.support.AuditLogTool] 修复的间接守护。
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
        // 一个只知 id 不知 name，另一个反之——链式合并把两半凑齐
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
        // 不注册任何 ISysAuditModule——initModule 直接返回
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
        // Kotlin 注解类可以直接当构造器调用——比反射构造再 inject 简洁得多
        val annotation = Audit(
            opType = OperationTypeEnum.CREATE,
            moduleCode = moduleCode,
        )
        return BaseLog(annotation)
    }
}
