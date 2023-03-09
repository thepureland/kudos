package io.kudos.base.bean

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

/**
 * BeanKit测试用例
 *
 * @author K
 * @since 1.0.0
 */
internal class BeanKitTest {

    private lateinit var person: Person

    @BeforeEach
    fun setUp() {
        person = Person().apply {
            id = "id"
            name = "Mike"
            sex = "male"
            age = 25
            birthday = Date(60528873600000L)
            address = Address().apply {
                province = "hunan"
                city = "changsha"
                street = "wuyilu"
                zipcode = "410000"
            }
            goods = listOf("sporting", "singing", "dancing")
            contact = mapOf("student" to "Tom", "teacher" to "Lucy")
            active = true
        }
    }

    @Test
    fun shallowClone() {
        val dest = io.kudos.base.bean.BeanKit.shallowClone(person)
        assertEquals(person, dest)
        assertTrue(person.address === dest.address) // 证明是浅克隆
        assertEquals(true, dest.active)
    }

    @Test
    fun deepClone() {
        val dest: Person = BeanKit.deepClone(person)
        assertEquals(person, dest)
        assertTrue(person.address !== dest.address) // 证明是深克隆
        assertEquals(true, dest.active)
    }

    @Test
    fun copyProperties() {
        val dest = Person()
        io.kudos.base.bean.BeanKit.copyProperties(person, dest)
        assertEquals(person, dest)
        assertTrue(person.address === dest.address) // 证明是浅克隆
        assertEquals(true, dest.active)
    }

    @Test
    fun extract() {
        val map: Map<String, Any?> = io.kudos.base.bean.BeanKit.extract(person)
        assertEquals(person.age, map["age"])
        assertEquals(person.name, map["name"])
        assertEquals(person.sex, map["sex"])
        assertTrue(person.address === map["address"])
        assertTrue(person.birthday === map["birthday"])
        assertTrue(person.contact === map["contact"])
        assertTrue(person.goods === map["goods"])
        assertEquals(true, person.active)
    }

    @Test
    fun getProperty() {
        assertEquals(person.age, io.kudos.base.bean.BeanKit.getProperty(person, "age"))
        assertEquals(person.goods?.get(0), io.kudos.base.bean.BeanKit.getProperty(person, "goods[0]"))
        assertEquals(person.contact?.get("student"), io.kudos.base.bean.BeanKit.getProperty(person, "contact(student)"))
        assertEquals(person.address?.province, io.kudos.base.bean.BeanKit.getProperty(person, "address.province"))
        assertEquals(true, io.kudos.base.bean.BeanKit.getProperty(person, "active"))
    }

    @Test
    fun setProperty() {
        io.kudos.base.bean.BeanKit.setProperty(person, "address.zipcode", "361000")
        assertEquals("361000", person.address?.zipcode)
        io.kudos.base.bean.BeanKit.setProperty(person, "isActive", "361000")
        assertEquals(true, person.active)
    }

    @Test
    fun copyPropertiesByMap() {
        val list = listOf("1", "2")
        val srcObj = Couple("key", list)
        val propertyMap = mapOf("first" to "second", "second[0]" to "first")

        val dest = io.kudos.base.bean.BeanKit.copyProperties(Couple::class, srcObj, propertyMap)
        assertEquals(srcObj.first, dest.second)
        assertEquals(srcObj.second!![0], dest.first)
    }

    @Test
    fun copyPropertiesToClassInstance() {
        val dest: Person = io.kudos.base.bean.BeanKit.copyProperties(Person::class, person)
        assertEquals(person, dest)
        assertTrue(person.address === dest.address) // 证明是浅克隆
        assertEquals(true, dest.active)
    }

    @Test
    fun copyPropertiesExcludeId() {
        val dest = Person()
        io.kudos.base.bean.BeanKit.copyPropertiesExcludeId(person, dest)
        assertEquals(person.age, dest.age)
        assertTrue(person.address === dest.address)
        assertNull(dest._getId())
        assertEquals(true, dest.active)
    }

    @Test
    fun copyPropertiesExclude() {
        val dest = Person()
        io.kudos.base.bean.BeanKit.copyPropertiesExclude(person, dest, "age", "address")
        assertEquals(person._getId(), dest._getId())
        assertEquals(0, dest.age)
        assertNull(dest.address)
        assertTrue(person.goods === dest.goods) // 浅克隆
        assertEquals(true, dest.active)
    }

    @Test
    fun resetPropertiesExcludeId() {
        val p: Person = io.kudos.base.bean.BeanKit.shallowClone(person)
        io.kudos.base.bean.BeanKit.resetPropertiesExcludeId(p)
        assertNull(p.name)
        assertNull(p.address)
        assertEquals(person._getId(), p._getId())
        assertNull(p.active)
    }

    @Test
    fun batchCopyProperties() {
        val persons = listOf<Person>(person, person)
        val results = io.kudos.base.bean.BeanKit.batchCopyProperties(Person::class, persons)
        assertEquals(persons.size.toLong(), results.size.toLong())
        assertEquals(persons[0].name, results[0].name)
        assertEquals(persons[1].weight, results[1].weight, 0.0)
        assertEquals(persons[1].active, results[1].active)
    }

    internal class Couple<F, S> {

        var first: F? = null
        var second: S? = null

        constructor()

        constructor(first: F?, second: S?) {
            this.first = first
            this.second = second
        }

    }
}