package io.kudos.ability.distributed.stream.common.support

import io.kudos.ability.distributed.stream.common.model.vo.StreamMessageVo
import java.io.ByteArrayInputStream
import java.io.InvalidClassException
import java.io.ObjectInputFilter
import java.io.ObjectInputStream

/**
 * Restricted reader for the legacy JDK message format. All broker and failure-handler reads share
 * this policy. Application DTOs (including nested types and serializable superclasses) require exact
 * class names in allowedClasses; package wildcards and caller-provided type names are never trusted.
 * Prefer schema-based formats for new integrations. Retain this only for wire compatibility.
 */
class StreamMessageSerialization {
    var allowedClasses: Set<String> = emptySet()
    var maxBytes: Int = 1_048_576
    var maxDepth: Long = 20
    var maxReferences: Long = 10_000
    var maxArrayLength: Long = 65_536

    fun deserialize(
        payload: ByteArray,
        targetClass: Class<*> = Any::class.java,
        additionalFilter: ObjectInputFilter? = null
    ): Any {
        require(maxBytes > 0 && maxDepth > 0 && maxReferences > 0 && maxArrayLength > 0) {
            "Stream deserialization limits must be positive"
        }
        require(payload.size <= maxBytes) { "Serialized message exceeds byte limit" }
        val allowed = BUILTIN_CLASSES + allowedClasses
        require(allowedClasses.none { '*' in it || '/' in it }) { "Stream allowedClasses requires exact class names" }
        val result = object : ObjectInputStream(ByteArrayInputStream(payload)) {
            override fun resolveProxyClass(interfaces: Array<String>): Class<*> {
                throw InvalidClassException("Serialized proxies are not accepted")
            }
        }.use { input ->
            input.setObjectInputFilter { info ->
                val baseline = when {
                    info.depth() > maxDepth || info.references() > maxReferences ||
                        info.streamBytes() > maxBytes || info.arrayLength() > maxArrayLength -> ObjectInputFilter.Status.REJECTED
                    info.serialClass() == null -> ObjectInputFilter.Status.UNDECIDED
                    else -> {
                        var type = info.serialClass()
                        val array = type.isArray
                        while (type.isArray) type = type.componentType
                        if (type.isPrimitive || type.name in allowed || (array && (type == Any::class.java || type.name == "java.util.Map\$Entry"))) {
                            ObjectInputFilter.Status.ALLOWED
                        } else ObjectInputFilter.Status.REJECTED
                    }
                }
                if (baseline == ObjectInputFilter.Status.REJECTED ||
                    additionalFilter?.checkInput(info) == ObjectInputFilter.Status.REJECTED) {
                    ObjectInputFilter.Status.REJECTED
                } else baseline
            }
            requireNotNull(input.readObject()) { "deserialize returned null" }
        }
        require(targetClass.isInstance(result)) { "Serialized message does not match the expected type" }
        return result
    }

    companion object {
        private val BUILTIN_CLASSES = setOf(
            StreamMessageVo::class.java.name,
            "java.lang.String", "java.lang.Boolean", "java.lang.Byte", "java.lang.Short",
            "java.lang.Integer", "java.lang.Long", "java.lang.Float", "java.lang.Double",
            "java.lang.Character", "java.lang.Number", "java.lang.Enum",
            "java.util.ArrayList", "java.util.LinkedList", "java.util.HashMap", "java.util.LinkedHashMap",
            "java.util.HashSet", "java.util.LinkedHashSet", "java.util.Date", "java.util.UUID",
            "java.math.BigDecimal", "java.math.BigInteger",
            "java.time.Ser", "java.time.LocalDate", "java.time.LocalTime", "java.time.LocalDateTime",
            "java.time.Instant", "java.time.OffsetDateTime", "java.time.ZonedDateTime", "java.time.ZoneOffset"
        )
    }
}
