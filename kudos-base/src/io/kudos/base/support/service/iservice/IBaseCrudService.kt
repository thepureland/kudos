package io.kudos.base.support.service.iservice

import io.kudos.base.query.Criteria
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.model.payload.ISearchPayload
import io.kudos.base.model.payload.UpdatePayload

/**
 * Basic business operation interface.
 *
 * Defines the complete set of business-layer operations, including query, insert, update, and delete.
 * Extends IBaseReadOnlyService, adding data modification capabilities on top of read-only operations.
 * Based on relational database tables, it provides a unified business interface to simplify business-layer code.
 *
 * Core features:
 * 1. Query operations: all query methods inherited from IBaseReadOnlyService
 * 2. Insert operations: support single and batch inserts; support inserting specified or excluded properties
 * 3. Update operations: support single and batch updates; support conditional updates and updating specified properties
 * 4. Delete operations: support deleting by primary key, by condition, and in batch
 *
 * Insert operations:
 * - insert: insert an entity or an insert-payload object and return the primary key value
 * - insertOnly: insert only the specified properties
 * - insertExclude: insert all properties except the specified ones
 * - batchInsert: batch insert with batching support
 *
 * Update operations:
 * - update: update an entity or an update-payload object; only updates changed properties
 * - updateWhen: conditional update; only updates when additional query conditions are met
 * - updateProperties: update the specified properties (Map form)
 * - updateOnly: update only the specified properties
 * - updateExcludeProperties: update all properties except the specified ones
 * - batchUpdate: batch update with batching support
 *
 * Delete operations:
 * - deleteById: delete by primary key
 * - delete: delete an entity object
 * - batchDelete: batch delete by primary key list or by condition
 *
 * Property control:
 * - Supports operating on specified properties only (Only methods)
 * - Supports excluding specified properties (Exclude methods)
 * - The primary key property is never updated
 *
 * Conditional updates:
 * - updateWhen methods support additional query conditions
 * - The update is performed only when the conditions are met
 * - Returns whether the update succeeded or the number of updated records
 *
 * Batch processing:
 * - All batch operations support a countOfEachBatch parameter
 * - Defaults to 1000 records per batch to avoid out-of-memory issues
 * - Implemented based on JDBC's executeBatch for performance
 *
 * Relationship with the DAO layer:
 * - Business-layer interface, typically delegating to the DAO layer implementation
 * - Business logic such as data validation and transaction control can be added at the business layer
 * - Provides a unified business interface that hides DAO-layer details
 *
 * Use cases:
 * - Business-layer data operations
 * - Service layer of RESTful APIs
 * - Batch data import and update
 *
 * Notes:
 * - Insert operations return the primary key value; update and delete operations return success status or the number of records affected
 * - Batch operations support batching to avoid handling large amounts of data at once
 * - Conditional updates require query conditions; otherwise an exception will be thrown
 * - The primary key property is automatically excluded from update operations
 *
 * @param PK Entity primary key type
 * @param E Entity type; must implement the IIdEntity interface
 * @since 1.0.0
 */
interface IBaseCrudService<PK : Any, E : IIdEntity<PK>> : IBaseReadOnlyService<PK, E> {

    //region Insert

    /**
     * Inserts the specified entity or "insert-item payload" into the current table.
     *
     * @param any Entity object or insert-item payload
     * @return Primary key value
     * @author K
     * @since 1.0.0
     */
    fun insert(any: Any): PK

    /**
     * Saves the entity object, persisting only the specified properties.
     *
     * @param entity Entity object
     * @param propertyNames Vararg of property names to save
     * @return Primary key value
     * @author K
     * @since 1.0.0
     */
    fun insertOnly(entity: E, vararg propertyNames: String): PK

    /**
     * Saves the entity object, excluding the specified properties.
     *
     * @param entity Entity object
     * @param excludePropertyNames Vararg of property names to exclude
     * @return Primary key value
     * @author K
     * @since 1.0.0
     */
    fun insertExclude(entity: E, vararg excludePropertyNames: String): PK

    /**
     * Batch-inserts the specified entities or "insert-item payloads" into the current table.
     *
     * Under the hood, ktorm implements this method based on the native JDBC executeBatch function.
     *
     * @param objects Collection of entity objects or "insert-item payloads"
     * @param countOfEachBatch Batch size; defaults to 1000
     * @return Number of records successfully inserted
     * @author K
     * @since 1.0.0
     */
    fun batchInsert(objects: Collection<Any>, countOfEachBatch: Int = 1000): Int

    /**
     * Batch-saves entity objects, persisting only the specified properties.
     *
     * @param entities List of entity objects
     * @param countOfEachBatch Batch size; defaults to 1000
     * @param propertyNames Vararg of property names to save
     * @return Number of records saved
     * @author K
     * @since 1.0.0
     */
    fun batchInsertOnly(entities: Collection<E>, countOfEachBatch: Int = 1000, vararg propertyNames: String): Int

    /**
     * Batch-saves entity objects, excluding the specified properties.
     *
     * @param entities List of entity objects
     * @param countOfEachBatch Batch size; defaults to 1000
     * @param excludePropertyNames Vararg of property names to exclude
     * @return Number of records saved
     * @author K
     * @since 1.0.0
     */
    fun batchInsertExclude(
        entities: Collection<E>, countOfEachBatch: Int = 1000, vararg excludePropertyNames: String
    ): Int

    //endregion Insert


    //region Update

    /**
     * Updates the record corresponding to the specified entity or update payload.
     *
     * @param any Entity object or update payload (if an entity object, only changed properties are updated; if an update payload, all of its properties are updated)
     * @return Whether the update succeeded
     * @author K
     * @since 1.0.0
     */
    fun update(any: Any): Boolean

    /**
     * Conditionally updates an entity object (only when the given additional query conditions are met).
     *
     * @param entity Entity object
     * @param criteria Additional query conditions
     * @return Whether the record was updated
     * @throws IllegalArgumentException When the condition is empty
     * @author K
     * @since 1.0.0
     */
    fun updateWhen(entity: E, criteria: Criteria): Boolean

    /**
     * Updates only the specified properties of the entity.
     *
     * @param id         Primary key value
     * @param properties Map(propertyName, propertyValue)
     * @return Whether the update succeeded
     * @author K
     * @since 1.0.0
     */
    fun updateProperties(id: PK, properties: Map<String, *>): Boolean

    /**
     * Conditionally updates only the specified properties of the entity (only when the given additional query conditions are met).
     * Note: the id property is never updated.
     *
     * @param id         Primary key value
     * @param properties Map(propertyName, propertyValue)
     * @param criteria Additional query conditions
     * @return Whether the record was updated
     * @throws IllegalArgumentException When no query conditions are provided
     * @author K
     * @since 1.0.0
     */
    fun updatePropertiesWhen(id: PK, properties: Map<String, *>, criteria: Criteria): Boolean

    /**
     * Updates only the specified properties of the entity.
     *
     * @param entity     Entity object
     * @param propertyNames Vararg of property names to update
     * @return Whether the update succeeded
     * @author K
     * @since 1.0.0
     */
    fun updateOnly(entity: E, vararg propertyNames: String): Boolean

    /**
     * Conditionally updates only the specified properties of the entity (only when the given additional query conditions are met).
     * Note: the id property is never updated.
     *
     * @param entity     Entity object
     * @param criteria Additional query conditions
     * @param propertyNames Vararg of property names to update
     * @return Whether the record was updated
     * @throws IllegalArgumentException When no query conditions are provided
     * @author K
     * @since 1.0.0
     */
    fun updateOnlyWhen(entity: E, criteria: Criteria, vararg propertyNames: String): Boolean

    /**
     * Conditionally updates all properties of the entity except the specified ones (only when the given additional query conditions are met).
     * Note: the id property is never updated.
     *
     * @param entity            Entity object
     * @param criteria Additional query conditions
     * @param excludePropertyNames Vararg of property names to exclude from the update
     * @return Whether the record was updated
     * @throws IllegalArgumentException When no query conditions are provided
     * @author K
     * @since 1.0.0
     */
    fun updateExcludePropertiesWhen(entity: E, criteria: Criteria, vararg excludePropertyNames: String): Boolean

    /**
     * Batch-updates the records corresponding to the entities.
     *
     * Under the hood, ktorm implements this method based on the native JDBC executeBatch function.
     *
     * @param entities Collection of entity objects
     * @param countOfEachBatch Batch size; defaults to 1000
     * @return Number of records successfully updated
     * @throws IllegalStateException When a primary key is null
     * @author K
     * @since 1.0.0
     */
    fun batchUpdate(entities: Collection<E>, countOfEachBatch: Int = 1000): Int

    /**
     * Conditionally batch-updates the specified properties.
     * For update rules see @see UpdatePayload, and for query rules see @see SearchPayload.
     *
     * @param S Search payload type
     * @param updatePayload Update payload
     * @return Number of updated records
     * @throws IllegalArgumentException When no query conditions are provided
     * @author K
     * @since 1.0.0
     */
    fun <S : ISearchPayload> batchUpdateWhen(updatePayload: UpdatePayload<S>): Int

    /**
     * Conditionally batch-updates entity objects (only when the given additional query conditions are met).
     *
     * @param entities Collection of entity objects
     * @param criteria Additional query conditions
     * @param countOfEachBatch Batch size; defaults to 1000
     * @return Number of updated records
     * @throws IllegalArgumentException When no query conditions are provided
     * @author K
     * @since 1.0.0
     */
    fun batchUpdateWhen(entities: Collection<E>, criteria: Criteria, countOfEachBatch: Int = 1000): Int

    /**
     * Updates all properties of the entity except the specified ones.
     * Note: the id property is never updated.
     *
     * @param entity            Entity object
     * @param excludePropertyNames Vararg of property names to exclude from the update
     * @return Whether the update succeeded
     * @author K
     * @since 1.0.0
     */
    fun updateExcludeProperties(entity: E, vararg excludePropertyNames: String): Boolean

    /**
     * Batch-updates entity objects, modifying only the specified properties.
     *
     * @param criteria   Query conditions
     * @param properties Map(propertyName, propertyValue)
     * @return Whether the update succeeded
     * @throws IllegalArgumentException When no properties to update are specified or no query conditions are provided
     * @author K
     * @since 1.0.0
     */
    fun batchUpdateProperties(criteria: Criteria, properties: Map<String, *>): Int

    /**
     * Batch-updates the specified properties of entity objects.
     *
     * @param entities   List of entity objects
     * @param countOfEachBatch Batch size; defaults to 1000
     * @param propertyNames Vararg of property names to update
     * @return Number of updated records
     * @author K
     * @since 1.0.0
     */
    fun batchUpdateOnly(entities: Collection<E>, countOfEachBatch: Int = 1000, vararg propertyNames: String): Int

    /**
     * Conditionally batch-updates the specified properties of entity objects (only when the given additional query conditions are met).
     * Note: the id property is never updated.
     *
     * @param entities   List of entity objects
     * @param criteria Additional query conditions
     * @param countOfEachBatch Batch size; defaults to 1000
     * @param propertyNames Vararg of property names to update
     * @return Number of updated records
     * @throws IllegalArgumentException When no query conditions are provided
     * @author K
     * @since 1.0.0
     */
    fun batchUpdateOnlyWhen(
        entities: Collection<E>, criteria: Criteria, countOfEachBatch: Int = 1000, vararg propertyNames: String
    ): Int

    /**
     * Batch-updates all properties of the entity except the specified ones.
     * Note: the id property is never updated.
     *
     * @param entities   List of entity objects
     * @param countOfEachBatch Batch size; defaults to 1000
     * @param excludePropertyNames Vararg of property names to exclude from the update
     * @return Whether the update succeeded
     * @author K
     * @since 1.0.0
     */
    fun batchUpdateExcludeProperties(
        entities: Collection<E>, countOfEachBatch: Int = 1000, vararg excludePropertyNames: String
    ): Int

    /**
     * Conditionally batch-updates all properties of the entity except the specified ones (only when the given additional query conditions are met).
     * Note: the id property is never updated.
     *
     * @param entities   List of entity objects
     * @param criteria Additional query conditions
     * @param countOfEachBatch Batch size; defaults to 1000
     * @param excludePropertyNames Vararg of property names to exclude from the update
     * @return Whether the update succeeded
     * @throws IllegalArgumentException When no query conditions are provided
     * @author K
     * @since 1.0.0
     */
    fun batchUpdateExcludePropertiesWhen(
        entities: Collection<E>, criteria: Criteria, countOfEachBatch: Int = 1000, vararg excludePropertyNames: String
    ): Int

    //endregion Update


    //region Delete

    /**
     * Deletes the record corresponding to the specified primary key value.
     *
     * @param id Primary key value; the type must be one of: String, Int, Long
     * @return Whether the deletion succeeded
     * @author K
     * @since 1.0.0
     */
    fun deleteById(id: PK): Boolean

    /**
     * Batch-deletes entity objects by the specified primary keys.
     *
     * @param ids List of primary keys
     * @return Number of deleted records
     * @author K
     * @since 1.0.0
     */
    fun batchDelete(ids: Collection<PK>): Int

    /**
     * Batch-deletes entity objects by the specified query conditions.
     *
     * @param criteria Query conditions
     * @return Number of deleted records
     * @throws IllegalArgumentException When no query conditions are provided
     * @author K
     * @since 1.0.0
     */
    fun batchDeleteCriteria(criteria: Criteria): Int

    /**
     * Batch-deletes entity objects matching the specified conditions.
     *
     * @param searchPayload Search payload
     * @return Number of deleted records
     * @throws IllegalArgumentException When no query conditions are provided
     * @author K
     * @since 1.0.0
     */
    fun batchDeleteWhen(searchPayload: ISearchPayload): Int

    /**
     * Deletes the record corresponding to the entity.
     *
     * @param entity Entity
     * @return Whether the deletion succeeded
     * @author K
     * @since 1.0.0
     */
    fun delete(entity: E): Boolean

    //endregion Delete

}