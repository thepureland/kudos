package io.kudos.base.data.json

import io.kudos.base.bean.Address
import io.kudos.base.bean.Person
import io.kudos.base.time.toLocalDate
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * JsonKit测试用例
 *
 * @author K
 * @since 1.0.0
 */
internal class JsonKitTest {

    private lateinit var person: Person

    @BeforeTest
    fun setUp() {
        person = Person().apply {
            id = "id"
            name = "Mike"
            sex = "male"
            age = 25
            birthday = Date(60528873600).toLocalDate()
            address = Address().apply {
                province = "hunan"
                city = "changsha"
                street = "wuyilu"
                zipcode = "410000"
            }
            goods = listOf("sporting", "singing", "dancing")
            contact = mapOf("student" to "Tom", "teacher" to "Lucy")
        }
    }

    @Test
    fun getPropertyValue() {
        val jsonStr = """
            {
                "active": true ,
                "status" :  1,
                "des":  "description " ,
                "message": "oh, err msg"
            }
        """
        assertEquals(true, JsonKit.getPropertyValue(jsonStr, "active"))
        assertEquals(1L, JsonKit.getPropertyValue(jsonStr, "status"))
        assertEquals("description ", JsonKit.getPropertyValue(jsonStr, "des"))
        assertEquals("oh, err msg", JsonKit.getPropertyValue(jsonStr, "message"))
        assertEquals(null, JsonKit.getPropertyValue(jsonStr, "no_exist"))
    }

    @Test
    fun jsonToDisplay() {
        assertEquals("A:b,B:b", JsonKit.jsonToDisplay("""{"A":"b","B":'b'}"""))
        assertEquals("", JsonKit.jsonToDisplay(" "))
    }

    @Test
    fun fromJson() {
        assertEquals(null, JsonKit.fromJson<Person>(""))
        assertEquals(null, JsonKit.fromJson<Person>("sdfsdf"))

        val json = "[\"abc\",\"cde\"]"
        val strings = JsonKit.fromJson<Array<String>>(json)!!
        assertEquals(strings[0], "abc")
        assertEquals(strings[1], "cde")
    }

    @Test
    fun testFromJson() {
        val jsonStr =
            """[{"id":"id","parentId":null,"children":[],"name":"Mike","sex":"male","age":25,"weight":0.0,"birthday":60528873600,"address":{"province":"hunan","city":"changsha","street":"wuyilu","zipcode":"410000"},"goods":["sporting","singing","dancing"],"contact":{"student":"Tom","teacher":"Lucy"},"active":null}]"""
        val persons = JsonKit.fromJson<List<Person>>(jsonStr)
        assertEquals(1, persons!!.size)
    }

    @Test
    fun testToJson() {
        val jsonStr =
            """[{"id":"id","parentId":null,"children":[],"name":"Mike","sex":"male","age":25,"weight":0.0,"birthday":"1971-12-02","address":{"province":"hunan","city":"changsha","street":"wuyilu","zipcode":"410000"},"goods":["sporting","singing","dancing"],"contact":{"student":"Tom","teacher":"Lucy"},"active":null}]"""
        assertEquals(jsonStr.length, JsonKit.toJson(listOf(person), true).length)
    }

    @Test
    fun updateBean() {
        val jsonStr =
            """{"id":null,"parentId":null,"name":"Mike","sex":"male","age":25,"weight":0.0,"birthday":60528873600,"address":{"province":"hunan","city":"changsha","street":"wuyilu","zipcode":"410000"},"goods":["sporting","singing","dancing"],"contact":{"student":"Tom","teacher":"Lucy"}}"""
        val person = Person().apply {
            name = "unknown"
            address = Address().apply {
                province = "unknown"
            }
        }
        val result = JsonKit.updateBean(jsonStr, person)!!
        assertEquals("Mike", result.name)
        assertEquals("hunan", result.address!!.province)
    }

    @Test
    fun toJsonP() {
        val expected =
            """func({"id":"id","parentId":null,"children":[],"name":"Mike","sex":"male","age":25,"weight":0.0,"birthday":"1971-12-02","address":{"province":"hunan","city":"changsha","street":"wuyilu","zipcode":"410000"},"goods":["sporting","singing","dancing"],"contact":{"student":"Tom","teacher":"Lucy"},"active":null})"""
        val fact = JsonKit.toJsonP("func", person, true)
        assertEquals(expected.length, fact.length)
    }

}