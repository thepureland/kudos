package io.kudos.base.data.json

import io.kudos.base.bean.Address
import io.kudos.base.bean.BeanKit
import io.kudos.base.bean.Person
import io.kudos.base.time.toLocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.util.*
import kotlin.reflect.typeOf
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * JsonKit测试用例
 *
 * @author K
 * @author AI: ChatGPT
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
            active = null
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

    // 用于结构对比解析（测试内专用，与你实际 Json 配置无关）
    private val parseJson = Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = true
    }

    // ---------- writeValueAsBytes：reified 版本（preserveNull = true） ----------
    @Test
    fun writeBytes_reified_preserveNull_true_personList_matchesKnownJson() {
        val expected = """
            [{"id":"id","parentId":null,"children":[],"name":"Mike","sex":"male","age":25,"weight":0.0,"birthday":"1971-12-02",
              "address":{"province":"hunan","city":"changsha","street":"wuyilu","zipcode":"410000"},
              "goods":["sporting","singing","dancing"],
              "contact":{"student":"Tom","teacher":"Lucy"},
              "active":null}]
        """.trimIndent().replace(Regex("\\s"), "") // 去空白仅用于展示；真正比较用结构

        val bytes = JsonKit.writeValueAsBytes(listOf(person), preserveNull = true)
        val actualElem = parseJson.parseToJsonElement(bytes.toString(Charsets.UTF_8))
        val expectedElem = parseJson.parseToJsonElement(expected)

        assertEquals(expectedElem, actualElem, "结构应与预期 JSON 一致（包含 null 字段）")
    }

    // ---------- writeValueAsBytes：Any 版本（preserveNull = false） ----------
    @Test
    fun writeBytes_any_preserveNull_false_person_omitsNulls() {
        val bytes = JsonKit.writeValueAsBytes(person, preserveNull = false)
        val obj = parseJson.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject

        // 基本字段存在
        assertEquals("id", obj["id"]?.toString()?.trim('"'))
        assertEquals("Mike", obj["name"]?.toString()?.trim('"'))

        // active=null 在 preserveNull=false 时应被省略
        assertFalse("active" in obj, "preserveNull=false 时应省略 null 字段")
    }

    // ---------- readValue：reified ----------
    @Test
    fun readValue_reified_roundtrip_person() {
        val bytes = JsonKit.writeValueAsBytes(person, preserveNull = true)

        // 如果 Person 有 @Serializable，则可直接反序列化；
        // 若没有，请为 Person 添加 @Serializable（或在你的工程里使用 KType 版本）
        val decoded: Person = JsonKit.readValue(bytes)

        assertEquals(person, decoded, "reified 版本应能与原对象等值（data class 建议）")
    }

    // ---------- readValue：KClass ----------
    @Test
    fun readValue_kClass_person() {
        val original = person
        val bytes = JsonKit.writeValueAsBytes(original, preserveNull = true)

        // 仅当 Person 本身可拿到直连序列化器（@Serializable 或 sealed 子类）时可用
        val decoded: Person = JsonKit.readValue(bytes, Person::class)
        assertEquals(original, decoded)
    }

    // ---------- readValue：KType（List<Person> / Map<String, Person>） ----------
    @Test
    fun readValue_kType_generic_list_and_map_of_person() {
        val p2 = BeanKit.deepClone(person)

        // List<Person>
        val listBytes = JsonKit.writeValueAsBytes(listOf(person, p2), preserveNull = true)
        val listType = typeOf<List<Person>>()
        val listDecoded = JsonKit.readValue(listBytes, listType) as List<*>
        assertEquals(listOf(person, p2), listDecoded)

        // Map<String, Person>
        val map = linkedMapOf("A" to person, "B" to p2)
        val mapBytes = JsonKit.writeValueAsBytes(map, preserveNull = true)
        val mapType = typeOf<Map<String, Person>>()
        @Suppress("UNCHECKED_CAST")
        val mapDecoded = JsonKit.readValue(mapBytes, mapType) as Map<String, Person>
        assertEquals(map, mapDecoded)
    }

}