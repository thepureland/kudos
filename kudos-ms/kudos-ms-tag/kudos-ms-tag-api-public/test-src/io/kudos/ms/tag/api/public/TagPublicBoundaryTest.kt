package io.kudos.ms.tag.api.public

import io.kudos.context.init.EnableKudos
import io.kudos.ms.tag.api.public.init.TagApiPublicApplication
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TagPublicBoundaryTest {
    @Test
    fun `public application is deployable and has no v1 business controllers`() {
        assertNotNull(TagApiPublicApplication::class.java.getAnnotation(EnableKudos::class.java))
        val classes = PathMatchingResourcePatternResolver()
            .getResources("classpath*:io/kudos/ms/tag/api/public/**/*.class")
            .map { it.filename.orEmpty() }

        assertEquals(emptyList(), classes.filter { it.endsWith("Controller.class") })
    }
}
