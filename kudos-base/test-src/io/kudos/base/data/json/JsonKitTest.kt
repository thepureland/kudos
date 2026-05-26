package io.kudos.base.data.json

import io.kudos.base.bean.Address
import io.kudos.base.bean.BeanKit
import io.kudos.base.bean.Person
import io.kudos.base.time.toLocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.util.Date
import kotlin.reflect.typeOf
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * JsonKit test cases
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

    // Used for structural comparison parsing (test-only; unrelated to the actual Json configuration)
    private val parseJson = Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = true
    }

    // ---------- writeValueAsBytes: reified variant (preserveNull = true) ----------
    @Test
    fun writeBytes_reified_preserveNull_true_personList_matchesKnownJson() {
        val expected = """
            [{"id":"id","parentId":null,"children":[],"name":"Mike","sex":"male","age":25,"weight":0.0,"birthday":"1971-12-02",
              "address":{"province":"hunan","city":"changsha","street":"wuyilu","zipcode":"410000"},
              "goods":["sporting","singing","dancing"],
              "contact":{"student":"Tom","teacher":"Lucy"},
              "active":null}]
        """.trimIndent().replace(Regex("\\s"), "") // Stripping whitespace is for display only; actual comparison is structural

        val bytes = JsonKit.writeValueAsBytes(listOf(person), preserveNull = true)
        val actualElem = parseJson.parseToJsonElement(bytes.toString(Charsets.UTF_8))
        val expectedElem = parseJson.parseToJsonElement(expected)

        assertEquals(expectedElem, actualElem, "Structure should match the expected JSON (including null fields)")
    }

    // ---------- writeValueAsBytes: Any variant (preserveNull = false) ----------
    @Test
    fun writeBytes_any_preserveNull_false_person_omitsNulls() {
        val bytes = JsonKit.writeValueAsBytes(person, preserveNull = false)
        val obj = parseJson.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject

        // Basic fields present
        assertEquals("id", obj["id"]?.toString()?.trim('"'))
        assertEquals("Mike", obj["name"]?.toString()?.trim('"'))

        // active=null should be omitted when preserveNull=false
        assertFalse("active" in obj, "Null fields should be omitted when preserveNull=false")
    }

    // ---------- readValue：reified ----------
    @Test
    fun readValue_reified_roundtrip_person() {
        val bytes = JsonKit.writeValueAsBytes(person, preserveNull = true)

        // If Person has @Serializable it can be deserialized directly;
        // otherwise, add @Serializable to Person (or use the KType variant in your project)
        val decoded: Person = JsonKit.readValue(bytes)

        assertEquals(person, decoded, "Reified variant should match the original object (data class recommended)")
    }

    // ---------- readValue：KClass ----------
    @Test
    fun readValue_kClass_person() {
        val original = person
        val bytes = JsonKit.writeValueAsBytes(original, preserveNull = true)

        // Only works when Person itself has a direct serializer (@Serializable or sealed subclass)
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