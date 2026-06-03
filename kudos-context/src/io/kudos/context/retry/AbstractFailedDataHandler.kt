package io.kudos.context.retry

import io.kudos.base.data.json.JsonKit
import io.kudos.base.lang.GenericKit
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import io.kudos.base.lang.string.RandomStringKit

/**
 * Abstract base class for failed-data handlers.
 *
 * Provides persistence and reading for failed data; subclasses implement the actual processing logic.
 *
 * Core features:
 * 1. Persistence: serialize failed data to JSON and save it to local files
 * 2. Reading: read JSON data from files and deserialize into objects
 * 3. Delegation: delegate the actual processing logic to subclasses
 *
 * File naming rules:
 * - Format: {timestamp}-{UUID}.json
 * - For example: 1704067200000-550e8400-e29b-41d4-a716-446655440000.json
 * - The timestamp enables ordering; the UUID guarantees uniqueness
 *
 * File storage structure:
 * - Root directory: {filePath()}
 * - Business directory: {filePath()}/{businessType}
 * - File path: {filePath()}/{businessType}/{timestamp}-{UUID}.json
 *
 * Notes:
 * - Subclasses must implement processFailedData to define the actual processing logic
 * - File I/O uses JSON for readability and recoverability
 * - Generics ensure type safety
 */
abstract class AbstractFailedDataHandler<T> : IFailedDataHandler<T> {

    /**
     * Persists failed data to a local file.
     *
     * Serializes failed data to JSON and writes it to the local file system.
     *
     * Flow:
     * 1. Build the file path: {filePath()}/{businessType}
     * 2. Create the directory if it does not exist
     * 3. Generate the file name: {timestamp}-{UUID}.json
     * 4. Serialize the data object into a JSON byte array
     * 5. Write the byte array to the file
     * 6. Return the absolute path of the file
     *
     * File naming:
     * - Timestamp: System.currentTimeMillis(), used for ordering
     * - UUID: RandomStringKit.uuid(), ensures uniqueness
     * - Format: timestamp-UUID.json
     *
     * Exception handling:
     * - On IO failure, throws RuntimeException wrapping the IOException
     * - Ensures the caller is aware of persistence failure
     *
     * @param data the failed-data object to persist
     * @return the absolute path of the saved file
     * @throws RuntimeException if the file operation fails
     */
    override fun persistFailedData(data: T): String = try {
        val dir = Paths.get(filePath()).resolve(businessType)
        if (Files.notExists(dir)) Files.createDirectories(dir)
        val file = dir.resolve("${System.currentTimeMillis()}-${RandomStringKit.uuid()}.json")
        Files.write(file, JsonKit.writeAnyAsBytes(data))
        file.toAbsolutePath().toString()
    } catch (e: IOException) {
        throw RuntimeException("Persist failed data error", e)
    }

    /**
     * Processes a failed-data file.
     *
     * Reads failed data from the file and calls the subclass's processFailedData method to handle it.
     *
     * Flow:
     * 1. Read the file: call readDataFromFile to read and deserialize the data
     * 2. Process the data: call the subclass-implemented processFailedData
     * 3. Return the result: whether processing succeeded
     *
     * Return value:
     * - true: processing succeeded; the caller will delete the file
     * - false: processing failed; the file is retained for the next retry
     *
     * @param file the failed-data file
     * @return true if processing succeeded, false otherwise
     */
    override fun handleFailedData(file: File): Boolean = processFailedData(readDataFromFile(file))

    /**
     * Handles the business data read from a file.
     *
     * Subclasses must implement this method to define the actual failed-data processing logic.
     *
     * Implementation requirements:
     * - Return true on success; the file will be deleted
     * - Return false on failure; the file is retained for the next retry
     * - If an exception is thrown, it is caught and logged by the upper layer
     *
     * @param data the data object read and deserialized from the file
     * @return true if processing succeeded, false otherwise
     */
    protected abstract fun processFailedData(data: T): Boolean

    /**
     * Reads a file and converts it into a T object.
     *
     * Reads JSON file contents and deserializes them into an object of the specified type.
     *
     * Flow:
     * 1. Read file bytes: use Files.readAllBytes to read all bytes
     * 2. Obtain generic type: use reflection to get the subclass's generic parameter type
     * 3. Deserialize: use JsonKit.readValue to deserialize the byte array into an object
     * 4. Type cast: cast the deserialized result to the generic type T
     *
     * Type retrieval:
     * - Uses GenericKit.getSuperClassGenricClass to get the generic type
     * - Supports obtaining the actual type argument at runtime
     *
     * Exception handling:
     * - On IO failure, throws RuntimeException wrapping the IOException
     * - On deserialization failure, throws the corresponding serialization exception
     *
     * @param file the file to read
     * @return the deserialized data object
     * @throws RuntimeException if reading or deserialization fails
     */
    @Suppress("UNCHECKED_CAST")
    protected fun readDataFromFile(file: File): T = try {
        val bytes = Files.readAllBytes(file.toPath())
        val dataType = GenericKit.getSuperClassGenricClass(this::class)
        JsonKit.readValue(bytes, dataType) as T
    } catch (e: IOException) {
        throw RuntimeException("Read failed data error", e)
    }

}
