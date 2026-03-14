package io.kudos.base.support.payload

import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.logic.AndOrEnum
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0

/**
 * 可变列表查询条件载体（final，不可再子类化）
 *
 * 继承 [ListSearchPayload]，并将 [ImmutableSearchPayload] 中的查询相关属性全部以 [var] 暴露，
 * 便于在**受信任的**服务端逻辑中动态组装、修改分页与排序以外的查询条件（如 [andOr]、[operators]、
 * [returnEntityClass]、[nullProperties]、[criterions] 等），获得灵活动态的查询能力。
 *
 * **仅限受信任场景使用**：因属性可被外部随时设值，本类**不得**用于封装来自不信任源的查询条件
 *（例如：直接接收客户端/开放 API 的请求体或参数并作为本类实例使用），否则存在篡改条件、越权或注入风险。
 * 不信任源的查询封装应使用 [ImmutableSearchPayload] 的子类（如各业务的 XxxQuery），在类中通过 [val] 明确定义。
 *
 * 典型用法：服务内部按需 new 本类实例，在代码中 set 各项条件后传入 DAO/Service 查询。
 *
 * @see ImmutableSearchPayload 不可变查询载体，适合不信任源
 * @see ListSearchPayload 列表查询载体基类（分页、排序等）
 * @author K
 * @since 1.0.0
 */
class MutableListSearchPayload : ListSearchPayload() {

    override var andOr: AndOrEnum = AndOrEnum.AND

    override var returnEntityClass: KClass<*>? = null

    override var nullProperties: List<String>? = null

    override var operators: Map<KProperty0<*>, OperatorEnum>? = null

    override var criterions: List<Criterion>? = null

}