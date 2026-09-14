package io.kudos.ms.tag.core.security

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.catalog.subjecttype.dao.TagSubjectTypeDao
import io.kudos.ms.tag.core.catalog.subjecttype.model.po.TagSubjectType
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TagAccessGuardTest {
    private val tenantGuard = TagTenantAccessGuard()
    private val subjectGuard = TagSubjectWriteGuard().apply {
        inject("tenantAccessGuard", tenantGuard)
        inject("subjectTypeDao", object : TagSubjectTypeDao() {
            override fun findByCode(code: String): TagSubjectType? = TagSubjectType().apply {
                id = "type-1"
                this.code = code
                name = "House"
                ownerServiceCode = "estate"
                active = true
                builtIn = false
                version = 0
            }
        })
    }

    @AfterTest
    fun clearContext() = KudosContextHolder.clear()

    @Test
    fun `ordinary caller is confined to its authenticated tenant`() {
        context(tenantId = "tenant-a", serviceCode = "estate")

        tenantGuard.requireTenant("tenant-a")
        assertFailsWith<TagAccessDeniedException> { tenantGuard.requireTenant("tenant-b") }
        assertFailsWith<TagAccessDeniedException> { tenantGuard.requireTenant(TagTenantAccessGuard.PLATFORM_TENANT_ID) }
    }

    @Test
    fun `explicit platform-management authority may cross tenant and use platform scope`() {
        context(tenantId = "platform", serviceCode = "management", platformManager = true)

        tenantGuard.requireTenant("tenant-b")
        tenantGuard.requireTenant(TagTenantAccessGuard.PLATFORM_TENANT_ID)
    }

    @Test
    fun `fact writer must own the subject type`() {
        context(tenantId = "tenant-a", serviceCode = "estate")
        val key = TagSubjectKey("tenant-a", "estate.house", "house-1")

        subjectGuard.requireWrite(key)

        context(tenantId = "tenant-a", serviceCode = "game")
        assertFailsWith<TagAccessDeniedException> { subjectGuard.requireWrite(key) }
    }

    @Test
    fun `missing security context fails closed`() {
        assertFailsWith<TagAccessDeniedException> { tenantGuard.requireTenant("tenant-a") }
    }

    private fun context(tenantId: String, serviceCode: String, platformManager: Boolean = false) {
        KudosContextHolder.set(KudosContext().apply {
            this.tenantId = tenantId
            atomicServiceCode = serviceCode
            if (platformManager) addOtherInfos(TagTenantAccessGuard.PLATFORM_MANAGEMENT_AUTHORITY to true)
        })
    }

    private fun Any.inject(field: String, value: Any) {
        val declared = this::class.java.getDeclaredField(field)
        declared.isAccessible = true
        declared.set(this, value)
    }
}
