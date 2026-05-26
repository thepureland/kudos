package io.kudos.ability.cache.common.batch.keyvalue

import kotlin.reflect.KFunction

/**
 * Batch cache key generator.
 *
 * @author K
 * @since 1.0.0
 */
interface IKeysGenerator {

    /**
     * Generates cache keys from the given method and parameters.
     *
     * @param target target instance
     * @param function the invoked method
     * @param params method parameters
     * @return list of generated keys
     * @author K
     * @since 1.0.0
     */
    fun generate(target: Any?, function: KFunction<*>?, vararg params: Any): List<String>

    /**
     * Returns the delimiter between key parts.
     *
     * @return delimiter
     * @author K
     * @since 1.0.0
     */
    fun getDelimiter(): String

    /**
     * Returns, in order, the indexes of parameters that compose the key.
     *
     * @param function method
     * @param params method parameters
     * @return list of parameter indexes
     * @author K
     * @since 1.0.0
     */
    fun getParamIndexes(function: KFunction<*>?, vararg params: Any): List<Int>

}