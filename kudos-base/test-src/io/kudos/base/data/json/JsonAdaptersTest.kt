package io.kudos.base.data.json

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * [JsonAdapters] time-serializer test cases.
 *
 * Exercises the LocalDate / LocalTime / LocalDateTime contextual serializers (both ISO-string and
 * epoch-millisecond decode branches and the error branches) via [JsonKit]'s `defaultJson` engine, which has the
 * java.time module registered. Also verifies the round-trip serialize path.
 *
 * @author K
 * @since 1.0.0
 */
internal class JsonAdaptersTest {

    @Serializable
    private data class Holder(
        @Contextual val date: LocalDate? = null,
        @Contextual val time: LocalTime? = null,
        @Contextual val dateTime: LocalDateTime? = null,
    )

    @Test
    fun serialize_roundtrip_iso_strings() {
        val h = Holder(
            date = LocalDate.of(2021, 1, 2),
            time = LocalTime.of(3, 4, 5),
            dateTime = LocalDateTime.of(2021, 1, 2, 3, 4, 5),
        )
        val json = JsonKit.toJson(h)
        // serialize writes ISO strings
        assert(json.contains("2021-01-02")) { json }
        assert(json.contains("03:04:05")) { json }
        val back = JsonKit.fromJson<Holder>(json)!!
        assertEquals(h, back)
    }

    @Test
    fun deserialize_from_iso_string() {
        val json = """{"date":"2022-06-15","time":"08:09:10","dateTime":"2022-06-15T08:09:10"}"""
        val h = JsonKit.fromJson<Holder>(json)!!
        assertEquals(LocalDate.of(2022, 6, 15), h.date)
        assertEquals(LocalTime.of(8, 9, 10), h.time)
        assertEquals(LocalDateTime.of(2022, 6, 15, 8, 9, 10), h.dateTime)
    }

    @Test
    fun deserialize_from_epoch_millis() {
        // pick a fixed epoch; convert using systemDefault to derive expectations (matches serializer impl)
        val epoch = 1_600_000_000_000L
        val zdt = Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault())
        val json = """{"date":$epoch,"time":$epoch,"dateTime":$epoch}"""
        val h = JsonKit.fromJson<Holder>(json)!!
        assertEquals(zdt.toLocalDate(), h.date)
        assertEquals(zdt.toLocalTime(), h.time)
        assertEquals(zdt.toLocalDateTime(), h.dateTime)
    }

    @Test
    fun deserialize_localDate_nonPrimitive_throws() {
        // LocalDate value as a JSON object is neither long nor parseable string -> SerializationException.
        // fromJson swallows SerializationException and returns null.
        val json = """{"date":{"x":1}}"""
        assertEquals(null, JsonKit.fromJson<Holder>(json))
    }

    @Test
    fun deserialize_localTime_nonPrimitive_throws_directly() {
        // The LocalTime/LocalDateTime serializers use .jsonPrimitive which throws on a non-primitive element;
        // decode directly so the exception surfaces (not swallowed by fromJson).
        val json = """{"time":[1,2]}"""
        assertFailsWith<Exception> {
            JsonKit.defaultJson.decodeFromString(Holder.serializer(), json)
        }
    }
}
