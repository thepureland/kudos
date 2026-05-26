package io.kudos.base.model.payload

import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.logic.AndOrEnum
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0


/**
 * Immutable query-condition payload (interface).
 *
 * Encapsulates query conditions and supports flexible rule configuration and result-type control.
 * Implementations automatically generate query conditions by **explicitly defining properties on the class**,
 * which simplifies query code.
 *
 * **Security design**: this interface only exposes methods such as [getAndOr]/[getOperators]; implementations
 * can only override these methods and cannot expose setters, so the query structure cannot be modified externally
 * after the instance is created. This makes the type suitable for carrying query parameters from untrusted
 * sources (e.g. frontend or open APIs), reducing injection and privilege-escalation risks.
 *
 * Core capabilities:
 * 1. Automatic condition generation: query conditions are generated from property values.
 * 2. Flexible operators: each property may use a different operator (subclasses return fixed values by overriding
 *    methods like [getOperators]).
 * 3. Logical-relation control: supports both AND and OR semantics.
 * 4. Result-type control: supports returning entities, single property values, or Maps.
 * 5. Custom conditions: fully customized query conditions are supported.
 *
 * Query rules:
 * 1. Null-value filtering: a property is only applied as a condition when its value is non-null.
 *    - Exception: properties listed in [getNullProperties] are used as conditions even when their value is null.
 * 2. Default operator: each property uses equality (EQ) by default.
 *    - Use [getOperators] to specify a different operator for a particular property.
 * 3. Logical relation: the relation between properties defaults to AND.
 *    - Override [getAndOr] to switch to OR.
 *
 * Result-type control:
 * - [getReturnProperties] is empty: returns a list of the entity specified by [getReturnEntityClass], or the PO list
 *   corresponding to the table.
 * - [getReturnProperties] contains a single property: returns a list of that property's values (e.g. List<String>).
 * - [getReturnProperties] contains multiple properties: returns a list of Map(propertyName, propertyValue).
 *
 * Priority:
 * - [getCriterions] (highest): fully custom query conditions; overrides all auto-generated conditions.
 * - [getOperators]: specifies operators for particular properties, overriding the default equality.
 * - Default rule: when the property value is non-null, use equality; properties are combined with AND.
 *
 * Use cases:
 * - Encapsulating query parameters for RESTful APIs (recommended: subclasses fix the structure; requests only
 *   populate property values).
 * - Scenarios that need to prevent external tampering with query conditions.
 *
 * Notes:
 * - Implementations need to define query properties; property names correspond to database columns or entity
 *   properties.
 * - The type returned by [getReturnEntityClass] may contain more properties than the PO, but only properties whose
 *   names match are populated automatically.
 * - [getNullProperties] is used for special scenarios where you need to query by null values.
 *
 * @author K
 * @since 1.0.0
 */
interface ISearchPayload {

    /**
     * Logical relation among properties; defaults to AND. Implementations customize this by overriding the method;
     * it must not be exposed as a mutable property.
     */
    fun getAndOr(): AndOrEnum = AndOrEnum.AND

    /** List of properties to be used as query conditions even when their value is null. */
    fun getNullProperties(): List<String>? = null

    /**
     * Custom (non-equality) operators for properties; keys must be property references (KProperty0) of this class.
     */
    fun getOperators(): Map<KProperty0<*>, OperatorEnum>? = null

    /**
     * The entity type to return.
     * Only applied when [getReturnProperties] is empty;
     * if it is null in that case, the PO corresponding to the queried table is returned.
     * The type may declare more properties than the PO, but only those with matching names (and compatible types)
     * are populated automatically.
     */
    fun getReturnEntityClass(): KClass<*>? = null

    /**
     * The property list to return.
     * If empty, returns a list of [getReturnEntityClass] objects or the PO list corresponding to the table;
     * if a single property, returns a list of that property's values;
     * if multiple properties, returns a list of Map(propertyName, propertyValue).
     */
    fun getReturnProperties(): List<String>? = null

    /** Fully custom property query logic; highest priority, overrides the original query logic. */
    fun getCriterions(): List<Criterion>? = null

}
