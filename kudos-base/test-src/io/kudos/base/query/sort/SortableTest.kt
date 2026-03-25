package io.kudos.base.query.sort

import kotlin.test.Test
import kotlin.test.assertEquals

internal class SortableTest {

    class OnProperty {
        @Sortable
        var name: String? = null

        @Sortable
        var code: String? = null

        var ignored: String? = null
    }

    class OnField {
        @field:Sortable
        var id: Long? = null
    }

    class OnGetter {
        var title: String? = null
            @Sortable
            get() = field
    }

    private data class DataClassPo(
        @Sortable val tagged: String = "",
        val plain: String = "",
    )

    @Test
    fun sortablePropertyNames_onDataClassPrimaryConstructor() {
        assertEquals(setOf("tagged"), sortablePropertyNamesForEntity(DataClassPo::class))
    }

    @Test
    fun sortablePropertyNames_onProperty_multiple() {
        assertEquals(setOf("name", "code"), sortablePropertyNamesForEntity(OnProperty::class))
    }

    @Test
    fun sortablePropertyNames_onField() {
        assertEquals(setOf("id"), sortablePropertyNamesForEntity(OnField::class))
    }

    @Test
    fun sortablePropertyNames_onGetter() {
        assertEquals(setOf("title"), sortablePropertyNamesForEntity(OnGetter::class))
    }
}
