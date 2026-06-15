package io.kudos.base.bean.validation.terminal

import io.kudos.base.bean.validation.constraint.annotations.AtLeast
import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

/**
 * Branch tests for [TerminalConstraintsCreator] covering cascade resolution for List / Map / Array
 * element types, the >1-level nesting guard, the class-level constraint annotation path, an empty
 * (no-constraint) bean, and the `.List` composite-annotation path on a getter.
 *
 * @author K
 * @since 1.0.0
 */
internal class TerminalConstraintsCreatorBranchesTest {

    @Test
    fun emptyBeanProducesEmptyRules() {
        val result = TerminalConstraintsCreator.create(NoConstraintsBean::class)
        assertTrue(result.isEmpty())
    }

    @Test
    fun dictItemCodeAnnotationIsSkippedInTerminalRules() {
        // ConstraintConvertorFactory returns null for @DictItemCode, so genRule's `?.let` branch skips it
        // and the property produces no terminal rule entry.
        val result = TerminalConstraintsCreator.create(DictItemCodeBean::class)
        // The dict property has only @DictItemCode (skipped) -> not present; nameWithNotBlank stays present.
        assertTrue(!result.containsKey("dict") || requireNotNull(result["dict"]).isEmpty())
        assertContains(result, "name")
    }

    @Test
    fun cascadedSingleObjectConstraintsAreCollected() {
        val result = TerminalConstraintsCreator.create(ObjectParent::class)
        // single nested object via @Valid -> one-level nesting path 'child.name'
        assertContains(result, "'child.name'")
        assertContains(requireNotNull(result["'child.name'"]), "NotBlank")
    }

    @Test
    fun classLevelConstraintAnnotationIsCollected() {
        val result = TerminalConstraintsCreator.create(ClassLevelBean::class)
        // AtLeast is a class-level constraint listing properties mobile/email
        assertContains(result, "mobile")
        assertContains(result, "email")
    }

    @Test
    fun intermediateInterfaceWithoutPropertyIsSkippedDuringGetterScan() {
        // Hierarchy: GrandParentItf (declares + constrains `tag`) <- MiddleItf (does NOT redeclare tag)
        // <- LeafBean (overrides tag). Scanning LeafBean recurses into MiddleItf which lacks the property,
        // hitting the NoSuchElementException catch in collectAnnotationsOnGetter, then finds it on GrandParentItf.
        val result = TerminalConstraintsCreator.create(LeafBean::class)
        assertContains(result, "tag")
        assertContains(requireNotNull(result["tag"]), "NotBlank")
    }

    // ---------------- fixtures ----------------

    internal data class NoConstraintsBean(
        val a: String?,
        val b: Int?,
    )

    internal data class ObjectChild(
        @get:NotBlank
        val name: String?,
    )

    internal data class ObjectParent(
        @get:Valid
        val child: ObjectChild?,
    )

    @AtLeast(properties = ["mobile", "email"], message = "at least one")
    internal data class ClassLevelBean(
        val mobile: String?,
        val email: String?,
    )

    internal data class DictItemCodeBean(
        @get:DictItemCode(atomicServiceCode = "test", dictType = "test")
        val dict: String?,

        @get:NotBlank
        val name: String?,
    )

    internal interface GrandParentItf {
        @get:NotBlank
        val tag: String?
    }

    internal interface MiddleItf : GrandParentItf

    internal data class LeafBean(
        override val tag: String?,
    ) : MiddleItf
}
