package io.kudos.ms.sys.core.microservice.model.po

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pure unit test for the [SysMicroService] Ktorm entity's custom `id` getter/setter, which delegate to `code`
 * (the entity has no separate id column; id is equivalent to code). Uses the in-memory entity factory only;
 * no Spring context or database required.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysMicroServiceTest {

    @Test
    fun id_getter_returnsCode() {
        val e = SysMicroService { code = "svc-a" }
        assertEquals("svc-a", e.id)
    }

    @Test
    fun id_setter_writesCode() {
        val e = SysMicroService { code = "old" }
        e.id = "new"
        assertEquals("new", e.code)
        assertEquals("new", e.id)
    }

    @Test
    fun setCode_reflectedInId() {
        val e = SysMicroService { code = "init" }
        e.code = "changed"
        assertEquals("changed", e.id)
    }
}
