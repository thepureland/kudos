package io.kudos.base.data.xml

import io.kudos.base.bean.Address
import io.kudos.base.lang.string.countMatches
import jakarta.xml.bind.annotation.*
import jakarta.xml.bind.annotation.adapters.XmlAdapter
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * XmlKit test cases
 *
 * @author K
 * @since 1.0.0
 */
internal class XmlKitTest {

    private lateinit var person: Person

    @BeforeTest
    fun setUp() {
        val address = Address().apply {
            province = "hunan"
            city = "changsha"
            street = "wuyilu"
            zipcode = "410000"
        }
        val goods = listOf("sporting", "singing", "dancing")
        val contact = mapOf("student" to "Tom", "teacher" to "Lucy")
        val birthday = LocalDate.of(2020, 8, 27)
        person = Person("Mike", "male", null, birthday, address, goods, contact)
    }

    @Test
    fun toXml() {
        var xml = XmlKit.toXml(person)
        println(xml)
        assert(xml.contains("<zipcode>410000</zipcode>"))

        val student = Student().apply {
            name = "Mike"
            sex = "male"
            weight = null
            birthday = LocalDate.of(2020, 8, 27)
            goods = listOf("sporting", "singing", "dancing")
            contact = mapOf("student" to "Tom", "teacher" to "Lucy")
        }
        xml = XmlKit.toXml(student)
        println(xml)
        assert(xml.contains("<goodses>"))
        assert(!xml.contains("<contact>"))
    }

    @Test
    fun testToXml() {
        val xml = XmlKit.toXml(listOf(person, person), "persons", Person::class)
        print(xml)
        assertEquals(2, xml.countMatches("<person>"))
    }

    @Test
    fun fromXml() {
        val xml = XmlKit.toXml(person)
        val p = XmlKit.fromXml(xml, Person::class)
        assertEquals(person.name, p.name)
        assertEquals(person.address, p.address)
        assertEquals(person.contact, p.contact)
    }

    @XmlRootElement // required
    internal data class Person(
        var name: String?,
        var sex: String?,
        @set:XmlElement(nillable = true, namespace = "") // will be mapped to an xml element
        var weight: Double?,
        @set:XmlJavaTypeAdapter(DateAdapter::class)
        var birthday: LocalDate?,
        var address: Address?,
        var goods: List<String>?,
        var contact: Map<String, String>?
    ) {
        constructor() : this(null, null, null, null, null, null, null)
    }

    @XmlRootElement(name = "student")
    @XmlAccessorType(XmlAccessType.PUBLIC_MEMBER) // only public properties are mapped to xml elements
    @XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL) // alphabetical order
    internal class Student {
        var name: String? = null
        var sex: String? = null

        @set:XmlAttribute(required = true) // also not mapped to an xml element
        var weight: Double? = null
        var birthday: LocalDate? = null
        var address: Address? = null

        @set:XmlElementWrapper(name = "goodses") // elements with the same name are wrapped as children of goodses
        var goods: List<String>? = null

        @set:XmlTransient  // not mapped to an xml element
        var contact: Map<String, String>? = null
    }

    private class DateAdapter : XmlAdapter<String, LocalDate>() {

        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        override fun unmarshal(date: String): LocalDate {
            return formatter.parse(date) as LocalDate
        }

        override fun marshal(date: LocalDate): String {
            return date.format(formatter)
        }

    }

}