package io.kudos.ms.sys.core.i18n.service

import io.kudos.ms.sys.common.i18n.vo.request.SysI18nFormUpdate
import io.kudos.ms.sys.core.i18n.cache.SysI18nHashCache
import io.kudos.ms.sys.core.i18n.dao.SysI18nDao
import io.kudos.ms.sys.core.i18n.model.po.SysI18n
import io.kudos.ms.sys.core.i18n.service.impl.SysI18NService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.context.ApplicationEventPublisher
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class SysI18nPathSecurityTest {
    private val dao = mock(SysI18nDao::class.java)
    private val events = mock(ApplicationEventPublisher::class.java)
    private val service = SysI18NService(dao, mock(SysI18nHashCache::class.java), events)

    @Test
    fun genericWritesRejectUnsafePathsBeforePersistence() {
        listOf("__proto__.polluted", "safe.constructor.x", "safe\n.__proto__.x").forEach { path ->
            val record = SysI18n { id = "entry"; key = path }
            assertFailsWith<IllegalArgumentException> { service.insert(record) }
            assertFailsWith<IllegalArgumentException> { service.update(record) }
        }
        verifyNoInteractions(dao, events)
    }

    @Test
    fun batchWritesValidateTheResolvedNamespaceAndEveryPathField() {
        val valid = SysI18nFormUpdate("entry", "zh_CN", "sys", "ui", "common", "label.name", "Name", null)
        listOf(
            valid.copy(key = "x.__proto__.y"),
            valid.copy(namespace = "constructor"),
            valid.copy(atomicServiceCode = "prototype"),
            valid.copy(namespace = "", i18nTypeDictCode = "__proto__"),
        ).forEach { form ->
            assertFailsWith<IllegalArgumentException> { service.batchSaveOrUpdate(listOf(form)) }
        }
        verifyNoInteractions(dao, events)
    }
}
