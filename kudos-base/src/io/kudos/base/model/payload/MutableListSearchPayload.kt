package io.kudos.base.model.payload

import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.logic.AndOrEnum
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0

/**
 * Mutable list query criteria payload (final, cannot be further subclassed).
 *
 * Extends [ListSearchPayload] and exposes the query-related results from the [ISearchPayload] interface via
 * writable backing fields, returning these fields via overrides of [getAndOr], [getOperators],
 * [getReturnEntityClass], [getNullProperties], [getCriterions], and [getReturnProperties]. This makes it easy to
 * dynamically assemble and modify query criteria in **trusted** server-side logic.
 *
 * **Use only in trusted scenarios**: because properties may be set externally at any time, this class **must not**
 * be used to wrap query criteria originating from untrusted sources (e.g. directly receiving a request body or
 * parameters from a client / open API and using it as an instance of this class), otherwise there is a risk of
 * criteria tampering, privilege escalation, or injection.
 * For untrusted-source query wrapping, use a class that implements [ISearchPayload] (e.g. per-business XxxQuery
 * classes), explicitly defined with [val].
 *
 * Typical usage: instantiate this class on demand inside a service, set criteria in code, then pass it to the
 * DAO/Service query.
 *
 * @see ISearchPayload immutable query payload interface, suitable for untrusted sources
 * @see ListSearchPayload base class for list query payloads (paging, sorting, etc.)
 * @author K
 * @since 1.0.0
 */
class MutableListSearchPayload : ListSearchPayload() {

    private var andOr: AndOrEnum = AndOrEnum.AND
    override fun getAndOr(): AndOrEnum = andOr
    fun setAndOr(value: AndOrEnum) { andOr = value }

    private var returnEntityClass: KClass<*>? = null
    override fun getReturnEntityClass(): KClass<*>? = returnEntityClass
    fun setReturnEntityClass(value: KClass<*>?) { returnEntityClass = value }

    private var nullProperties: List<String>? = null
    override fun getNullProperties(): List<String>? = nullProperties
    fun setNullProperties(value: List<String>?) { nullProperties = value }

    private var operators: Map<KProperty0<*>, OperatorEnum>? = null
    override fun getOperators(): Map<KProperty0<*>, OperatorEnum>? = operators
    fun setOperators(value: Map<KProperty0<*>, OperatorEnum>?) { operators = value }

    private var criterions: List<Criterion>? = null
    override fun getCriterions(): List<Criterion>? = criterions
    fun setCriterions(value: List<Criterion>?) { criterions = value }

    private var returnProperties: List<String>? = null
    override fun getReturnProperties(): List<String>? = returnProperties
    fun setReturnProperties(value: List<String>?) { returnProperties = value }

}