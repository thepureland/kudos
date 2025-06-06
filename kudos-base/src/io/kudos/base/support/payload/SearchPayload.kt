package io.kudos.base.support.payload

import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.logic.AndOrEnum
import kotlin.reflect.KClass


/**
 * 查询条件载体父类
 *
 * 查询规则:
 *    1. 各个属性只有当其值非空时才会应用该查询条件
 *    2. 各个属性的查询逻辑默认都是等于, 若有特殊需求由用户自行实现
 *    3. 各属性间的关系默认为AND, 可通过重写and属性改变
 *
 * @author K
 * @since 1.0.0
 */
open class SearchPayload {

    /** 各属性间的查询逻辑关系，默认为AND */
    open var andOr: AndOrEnum = AndOrEnum.AND

    /** 值为null的属性的列表 */
    open var nullProperties: List<String>? = null

    /**
     * 查询结果的属性列表
     * 查询结果类型:
     *    如果为空, 则为指定的returnEntityClass对象列表或所查询表对应的PO列表;
     *    如果单个属性, 则为该属性值的列表;
     *    如果多个属性, 则为Map(属性名, 属性值)的列表;
     */
    open var returnProperties: List<String>? = null

    /**
     * 返回的实体类型
     * 仅当 returnProperties 为空时才会应用,
     * 如果此时 returnEntityClass 为null, 将返回所查询表对应的PO.
     * 该类中定义的属性可以比PO的多,但是只会自动封装名字一致的(类型要能兼容).
     */
    open var returnEntityClass: KClass<*>? = null

    /**
     * 属性的特殊(非等于)查询操作逻辑，key必须为该类的属性名
     */
    open var operators: Map<String, OperatorEnum>? = null

    /**
     * 完全自定义的属性查询逻辑。优先级最高，会覆盖原来的查询逻辑！
     */
    open var criterions: List<Criterion>? = null

}