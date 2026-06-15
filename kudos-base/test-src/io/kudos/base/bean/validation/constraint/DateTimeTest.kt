package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.constraint.annotations.DateTime
import io.kudos.base.bean.validation.kit.ValidationKit
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * DateTime test cases
 *
 * @author K
 * @since 1.0.0
 */
internal class DateTimeTest {

    @Test
    fun validate() {
        val bean1 = TestDateTimeBean("2020-08-08", "08:08:08", "2020-08-08 08:08:08")
        assert(ValidationKit.validateBean(bean1, failFast = false).isEmpty())

        val bean2 = TestDateTimeBean("2020/08/08", "080808", "2020/08/08 08:08:08")
        assertEquals(3, ValidationKit.validateBean(bean2, failFast = false).size)

        // null values pass directly (null handling is delegated to @NotNull etc.)
        val bean3 = TestDateTimeBean(null, null, null)
        assert(ValidationKit.validateBean(bean3).isEmpty())
    }

    internal data class TestDateTimeBean(

        @get:DateTime(format = "yyyy-mm-dd")
        val birthday: String?,

        @get:DateTime(format = "hh:mm:ss")
        val schoolTime: String?,

        @get:DateTime(format = "yyyy-mm-dd hh:mm:ss")
        val createTime: String?

    )

}
