package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.constraint.annotations.NotNullOn
import io.kudos.base.bean.validation.kit.ValidationKit
import io.kudos.base.bean.validation.support.Depends
import org.hibernate.validator.constraints.Length
import kotlin.test.Test

/**
 * Test cases for NotNullOn.
 *
 * @author K
 * @since 1.0.0
 */
internal class NotNullOnTest {

    @Test
    fun validate() {
        // Expression does not hold; value may be null; when value is null, other annotations on the property are skipped
        val bean1 = TestNotNullOnBean(false, null)
        assert(ValidationKit.validateBean(bean1).isEmpty())

        // Expression does not hold; value may be null, but it is not null here; other annotations are still evaluated; failure case
        val bean2 = TestNotNullOnBean(false, "ABCDEF")
        assert(ValidationKit.validateBean(bean2).isNotEmpty())

        // Expression does not hold; value may be null, but it is not null here; other annotations are still evaluated; success case
        val bean3 = TestNotNullOnBean(false, "ABCD")
        assert(ValidationKit.validateBean(bean3).isEmpty())

        // Expression holds; value must not be null, but it is null here; validation fails
        val bean4 = TestNotNullOnBean(true, null)
        assert(ValidationKit.validateBean(bean4).isNotEmpty())

        // Expression holds; value must not be null and is not null here; other annotations are still evaluated; failure case
        val bean5 = TestNotNullOnBean(true, "ABCDEF")
        assert(ValidationKit.validateBean(bean5).isNotEmpty())

        // Expression holds; value must not be null and is not null here; other annotations are still evaluated; success case
        val bean6 = TestNotNullOnBean(true, "ABCD")
        assert(ValidationKit.validateBean(bean6).isEmpty())
    }


    internal data class TestNotNullOnBean(

        val validate: Boolean?,

        @get:NotNullOn(Depends(properties = ["validate"], values = ["true"]))
        @get:Length(min = 4, max = 4, message = "the captcha length must be 4")
        val captcha: String?

    )

}
