package io.kudos.base.support.payload

import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.logic.AndOrEnum
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0

/**
 * 可变列表查询条件载体（final，不可再子类化）
 *
 * 继承 [ListSearchPayload]，并将 [ISearchPayload] 接口中的查询相关结果以可写后备字段暴露，
 * 通过 override [getAndOr]、[getOperators]、[getReturnEntityClass]、[getNullProperties]、
 * [getCriterions]、[getReturnProperties] 返回这些字段，便于在**受信任的**服务端逻辑中动态组装、修改查询条件。
 *
 * **仅限受信任场景使用**：因属性可被外部随时设值，本类**不得**用于封装来自不信任源的查询条件
 *（例如：直接接收客户端/开放 API 的请求体或参数并作为本类实例使用），否则存在篡改条件、越权或注入风险。
 * 不信任源的查询封装应使用实现 [ISearchPayload] 的类（如各业务的 XxxQuery），在类中通过 [val] 明确定义。
 *
 * 典型用法：服务内部按需 new 本类实例，在代码中 set 各项条件后传入 DAO/Service 查询。
 *
 * @see ISearchPayload 不可变查询载体接口，适合不信任源
 * @see ListSearchPayload 列表查询载体基类（分页、排序等）
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