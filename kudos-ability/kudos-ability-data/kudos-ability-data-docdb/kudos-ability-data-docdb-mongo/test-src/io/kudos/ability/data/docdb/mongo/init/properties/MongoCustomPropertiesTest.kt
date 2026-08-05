package io.kudos.ability.data.docdb.mongo.init.properties

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [MongoCustomProperties].
 *
 * Locks in the default of the only toggle the module carries: `bigIntegerAsString` must default
 * to true (the loss-free encoding), and remain a plain mutable property so Spring's relaxed
 * binder can flip it from `kudos.ability.docdb.mongo.big-integer-as-string`.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class MongoCustomPropertiesTest {

    @Test
    fun bigIntegerAsString_defaultsToTrue() {
        assertTrue(
            MongoCustomProperties().bigIntegerAsString,
            "default must be true — String storage is the only loss-free BigInteger round trip",
        )
    }

    @Test
    fun bigIntegerAsString_isMutableForRelaxedBinding() {
        val props = MongoCustomProperties()
        props.bigIntegerAsString = false
        assertFalse(props.bigIntegerAsString)
        props.bigIntegerAsString = true
        assertTrue(props.bigIntegerAsString)
    }
}
