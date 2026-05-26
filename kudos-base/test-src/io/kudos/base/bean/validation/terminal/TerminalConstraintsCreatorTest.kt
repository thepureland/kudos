package io.kudos.base.bean.validation.terminal

import io.kudos.base.bean.validation.constraint.annotations.*
import io.kudos.base.bean.validation.support.Depends
import io.kudos.base.bean.validation.support.IBeanValidator
import io.kudos.base.bean.validation.support.RegExps
import io.kudos.base.bean.validation.support.SeriesTypeEnum
import io.kudos.base.data.json.JsonKit
import io.kudos.base.enums.impl.SexEnum
import io.kudos.base.support.logic.LogicOperatorEnum
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.hibernate.validator.constraints.*
import org.hibernate.validator.constraints.time.DurationMax
import org.hibernate.validator.constraints.time.DurationMin
import java.time.Duration
import java.time.LocalDate
import kotlin.test.assertContains
import kotlin.test.Test

/**
 * Test cases for TerminalConstraintsCreator.
 *
 * @author K
 * @since 1.0.0
 */
internal class TerminalConstraintsCreatorTest {

    @Test
    fun create() {
        val result = TerminalConstraintsCreator.create(TestRegisterBean::class)
        println(JsonKit.toJson(result))
    }

    @Test
    fun createFromParentInterfacePropertyConstraint() {
        val result = TerminalConstraintsCreator.create(TestChildFromInterface::class)
        assertContains(result, "code")
        assertContains(requireNotNull(result["code"]), "NotBlank")
        assertContains(requireNotNull(result["code"]), "Length")
    }

    @Test
    fun createFromOverriddenParentPropertyConstraint() {
        val result = TerminalConstraintsCreator.create(TestChildOverrideBean::class)
        assertContains(result, "name")
        assertContains(requireNotNull(result["name"]), "NotBlank")
        assertContains(requireNotNull(result["name"]), "Length")
    }

    @AtLeast(properties = ["mobile", "email"], message = "At least one contact method must be provided")
//    @ScriptAssert(lang = "javascript", script = "1==1") // Not yet supported by Kudos
    internal data class TestRegisterBean(

        @get:AssertTrue
        val validate: Boolean?,

        @get:AssertFalse
        val guest: Boolean?,

        @get:Null
        val error: String?,

        @get:NotBlank
        @get:CodePointLength(min = 6, max = 32, message = "Username length must be between 6 and 32 characters")
        @get:Remote(checkClass = UsernameValidator::class, requestUrl = "/isUserAvailable", message = "Username already exists")
        val username: String?,

        @get:NotNull
        @get:Length(min = 8, max = 32, message = "Password length must be between 8 and 32 characters")
        val password: String?,

        @get:Compare.List(
            Compare(
                depends = Depends(
                    properties = ["validate"],
                    values = ["true"]
                ),
                anotherProperty = "password",
                logic = LogicOperatorEnum.EQ,
                message = "Passwords do not match"
            ),
            Compare(
                anotherProperty = "username",
                logic = LogicOperatorEnum.IN,
                message = "Password must not contain the username"
            )

        )
        val confirmPassword: String?,

        @get:Pattern(regexp = RegExps.Communication.CN_MAINLAND_MOBILE, message = "Invalid mobile number format")
        val mobile: String?,

        @get:Email
        val email: String?,

        @get:Min(18, message = "Users under 18 cannot register")
        @get:Max(60, message = "Users over 60 cannot register")
        val age: Int?,

        @get:Past
        val graduateDate: LocalDate?,

        @get:Future
        val expireDate: LocalDate?,

        @get:PastOrPresent
        val date1: LocalDate?,

        @get:FutureOrPresent
        val date2: LocalDate?,

        @get:DurationMax
        val time1: Duration?,

        @get:DurationMin
        val time2: Duration?,

        @get:DecimalMin("50.0", message = "Weight must be greater than 50.0KG")
        @get:DecimalMax("100.0", message = "Weight must be less than 100.0KG")
        val weight: Double?,

        @get:Range(min = 30, max = 270, message = "Height must be between 30cm and 270cm")
        val height: Double?,

        @get:Positive(message = "Eyesight value must be positive")
        @get:Digits(integer = 1, fraction = 1, message = "Eyesight value must have 1 integer digit and 1 fractional digit")
        val eyesight: Double?,

        @get:Negative
        val value1: Double?,

        @get:NegativeOrZero
        val value2: Double?,

        @get:PositiveOrZero
        val value3: Double?,

        @get:CreditCardNumber
        val creditCardNumber: String?,

//        @get:Currency
//        val currency: MonetaryAmount?,

        @get:EAN
        val barcode: String?,

        @get:LuhnCheck
        val string1: String?,

        @get:Mod10Check
        val string2: String?,

        @get:Mod11Check
        val string3: String?,

        @get:ISBN
        val bookIsbn: String?,

        @get:ParameterScriptAssert(lang = "javascript", script = "1==1")
        val richText: String?,

        @get:URL
        val photo: String?,

        @get:Size(min = 3, max = 6, message = "You must select between 3 and 6 hobbies")
        val hobbies: Array<String>?,

        @get:DictEnumItemCode(enumClass = SexEnum::class, message = "Invalid gender")
        val sex: String,

        @get:NotEmpty
        @get:Series(type = SeriesTypeEnum.INC_DIFF, step = 2.0, message = "Incorrect answer to bot-detection question")
        val question: Array<Int>?,

        @get:NotNullOn(Depends(properties = ["age"], logics = [LogicOperatorEnum.GE], values = ["18"]))
        val job: String?,

        @get:Each(
            Constraints(
                order = [NotBlank::class, Pattern::class],
                notBlank = NotBlank(), message = "No ability may be blank",
                pattern = Pattern(regexp = "[a-zA-Z]+", message = "Ability must consist of English letters")
            )
        )
        val abilities: Array<String>?,

        @get:Exist(
            Constraints(notBlank = NotBlank()),
            message = "At least one security question must be filled in"
        )
        @get:UniqueElements
        val safeQuestions: List<String?>?,

        @get:Constraints(
            order = [NotBlank::class, Pattern::class],
            notBlank = NotBlank(message = "Remark must not be blank"),
            pattern = Pattern(regexp = "[a-zA-Z0-9]+", message = "Remark must not contain special characters")
        )
        val remark: String?,


        @get:Valid
        val address: Address

    ) {
        override fun equals(other: Any?): Boolean = true
        override fun hashCode(): Int = 0
    }

    internal data class Address(

        @get:NotNull
        val country: String?,

        @get:NotNull
        val province: String?,

        val city: String?

    )

    internal class UsernameValidator: IBeanValidator<TestRegisterBean> {

        override fun validate(bean: TestRegisterBean): Boolean {
            // Check whether the username is available
            return true
        }

    }

    internal interface TestParentInterface {

        @get:NotBlank
        @get:Length(max = 16)
        val code: String?

    }

    internal data class TestChildFromInterface(
        override val code: String?
    ) : TestParentInterface

    internal interface TestParentBean {

        @get:NotBlank
        @get:Length(max = 32)
        val name: String?

    }

    internal data class TestChildOverrideBean(
        override val name: String?
    ) : TestParentBean

}
