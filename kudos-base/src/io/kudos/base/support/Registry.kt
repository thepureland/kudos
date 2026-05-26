package io.kudos.base.support

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Object registry.
 *
 * Provides key-based object registration and lookup, supporting multiple objects per key.
 *
 * Core features:
 * 1. Object registration: register objects by key; supports single and batch registration
 * 2. Object lookup: look up the list of registered objects by key
 * 3. Deduplication: automatically deduplicates to avoid registering the same object twice
 *
 * Data structure:
 * - Uses ConcurrentHashMap for storage, with key as String and value as CopyOnWriteArrayList<Any>
 * - Supports multiple objects per key
 * - Thread-safe; supports concurrent access
 *
 * Use cases:
 * - Plugin registration: register plugin instances by plugin type
 * - Strategy pattern: register strategy implementations by strategy type
 * - Extension points: register extension implementations by extension point name
 *
 * Notes:
 * - Uses ConcurrentHashMap to ensure thread safety
 * - Object comparison uses the equals method; ensure objects implement equals correctly
 * - If the key does not exist, lookup returns an empty list rather than null
 *
 * @author K
 * @since 1.0.0
 */
object Registry {

    /**
     * Map of all registered objects.
     * The key is the registration key, and the value is the list of registered objects.
     */
    private val map = ConcurrentHashMap<String, CopyOnWriteArrayList<Any>>()

    /**
     * Looks up registered objects by key.
     *
     * Finds all registered objects for the specified key.
     *
     * Workflow:
     * 1. Look up the list corresponding to the key in the map
     * 2. If it exists, return a read-only snapshot of the list
     * 3. If it does not exist, return an empty list (not null)
     *
     * Return value:
     * - If objects are registered under the key, returns a read-only list containing all of them
     * - If the key is not registered, returns an empty list (not null)
     *
     * Note: the returned list is a snapshot that does not expose the registry's internal mutable state.
     *
     * @param key The registration key
     * @return The list of registered objects; returns an empty list if the key does not exist
     */
    fun lookup(key: String): List<Any> = map[key]?.toList() ?: emptyList()

    /**
     * Registers a single object.
     *
     * Registers the object under the specified key; does not add the object again if it already exists.
     *
     * Workflow:
     * 1. Look up the list for the key (create an empty list if it does not exist)
     * 2. Check whether the object already exists (using the contains method)
     * 3. Add it to the list if it does not already exist
     * 4. Put the list back into the map (ensuring the map reference is up to date)
     *
     * Deduplication:
     * - Uses the contains method to check whether the object already exists
     * - Comparison is based on the equals method
     * - If the object already exists, it will not be added again
     *
     * Notes:
     * - Objects must implement equals correctly; otherwise deduplication may fail
     * - The map reference is updated even when the object already exists
     * - Thread-safe; supports concurrent registration
     *
     * @param key The registration key
     * @param obj The object to register
     */
    fun register(key: String, obj: Any) {
        val resultList = map.computeIfAbsent(key) { CopyOnWriteArrayList() }
        resultList.addIfAbsent(obj)
    }

    /**
     * Batch registers objects.
     *
     * Registers multiple objects under the specified key; returns immediately if the array is empty.
     *
     * Workflow:
     * 1. Check whether the array is empty and return immediately if so
     * 2. Look up the list for the key (create an empty list if it does not exist)
     * 3. Convert the array to a list and add it to the result list
     * 4. Put the list back into the map
     *
     * Batch handling:
     * - Uses addAll to add all objects at once
     * - Does not perform deduplication (unlike single-object registration)
     * - Duplicate objects in the array are all added
     *
     * Notes:
     * - If the array is empty, returns immediately without performing any operation
     * - Batch registration does not check whether objects already exist; duplicates may be added
     * - If deduplication is required, handle it before calling
     *
     * @param key The registration key
     * @param objs The vararg array of objects to register
     */
    fun register(key: String, vararg objs: Any) {
        if (objs.isEmpty()) {
            return
        }
        val resultList = map.computeIfAbsent(key) { CopyOnWriteArrayList() }
        resultList.addAll(listOf(*objs))
    }
}