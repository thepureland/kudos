package io.kudos.base.lang

import org.apache.commons.lang3.SerializationUtils
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable

/**
 * Serialization utility.
 *
 * @author K
 * @since 1.0.0
 */
object SerializationKit {

    // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    // Wrapper for org.apache.commons.lang3.SerializationUtils
    // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

    /**
     * Deep-clone the specified object.
     * This method is many times slower than overriding clone on every object in the object graph. However, for complex object graphs, or objects that do not support deep cloning, this offers an alternative. Of course, all objects must implement the `Serializable` interface.
     *
     * @param T the type of the object to clone
     * @param obj the Serializable object to clone
     * @return the cloned object
     * @throws org.apache.commons.lang3.SerializationException (runtime) if serialization fails
     * @author K
     * @since 1.0.0
     */
    fun <T : Serializable?> clone(obj: T): T = SerializationUtils.clone(obj)

    /**
     * Serialize an object to the specified output stream.
     * The output stream is closed when the object has been written, eliminating the need for a finally clause or exception handling in application code.
     * The supplied stream is not buffered inside the method. That is your application's responsibility if needed.
     *
     * @param obj the object to serialize to bytes, may be null
     * @param outputStream the stream to write to, must not be null
     * @throws IllegalArgumentException if `outputStream` is `null`
     * @throws org.apache.commons.lang3.SerializationException (runtime) if serialization fails
     * @author K
     * @since 1.0.0
     */
    fun serialize(obj: Serializable?, outputStream: OutputStream?) = SerializationUtils.serialize(obj, outputStream)

    /**
     * Serialize an object to a byte array.
     *
     * @param obj the object to serialize to bytes, may be null
     * @return the byte array
     * @throws org.apache.commons.lang3.SerializationException (runtime) if serialization fails
     * @author K
     * @since 1.0.0
     */
    fun serialize(obj: Serializable?): ByteArray = SerializationUtils.serialize(obj)

    /**
     * Deserialize an object from an input stream.
     * The input stream is closed when the object has been read, eliminating the need for a finally clause or exception handling in application code.
     * The supplied stream is not buffered inside the method. That is your application's responsibility if needed.
     *
     * @param inputStream the input stream, must not be null
     * @return the deserialized object
     * @throws IllegalArgumentException if `inputStream` is `null`
     * @throws org.apache.commons.lang3.SerializationException (runtime) if deserialization fails
     * @author K
     * @since 1.0.0
     */
    fun deserialize(inputStream: InputStream?): Any = SerializationUtils.deserialize(inputStream)

    /**
     * Deserialize an object from a byte array.
     *
     * @param objectData the byte array, must not be null
     * @return the deserialized object
     * @throws org.apache.commons.lang3.SerializationException (runtime) if deserialization fails
     * @author K
     * @since 1.0.0
     */
    fun deserialize(objectData: ByteArray): Any? = SerializationUtils.deserialize(objectData)

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // Wrapper for org.apache.commons.lang3.SerializationUtils
    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
}
