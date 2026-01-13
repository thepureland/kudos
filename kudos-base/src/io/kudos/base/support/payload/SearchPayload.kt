package io.kudos.base.support.payload

import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.logic.AndOrEnum
import kotlin.reflect.KClass


/**
 * 查询条件载体父类
 * 
 * 用于封装查询条件，支持灵活的查询规则配置和结果类型控制。
 * 子类通过定义属性来自动生成查询条件，简化查询代码的编写。
 * 
 * 核心功能：
 * 1. 自动条件生成：根据属性值自动生成查询条件
 * 2. 灵活操作符：支持为每个属性指定不同的操作符
 * 3. 逻辑关系控制：支持AND和OR两种逻辑关系
 * 4. 结果类型控制：支持返回实体、属性值或Map等多种结果类型
 * 5. 自定义条件：支持完全自定义的查询条件
 * 
 * 查询规则：
 * 1. 空值过滤：各个属性只有当其值非空时才会应用该查询条件
 *    - 例外：nullProperties中指定的属性，即使值为null也会作为查询条件
 * 2. 默认操作符：各个属性的查询逻辑默认都是等于（EQ）
 *    - 可通过operators Map为特定属性指定不同的操作符
 * 3. 逻辑关系：各属性间的关系默认为AND
 *    - 可通过重写andOr属性改变为OR
 * 
 * 结果类型控制：
 * - returnProperties为空：返回returnEntityClass指定的实体列表，或表对应的PO列表
 * - returnProperties为单个属性：返回该属性值的列表（如List<String>）
 * - returnProperties为多个属性：返回Map(属性名, 属性值)的列表
 * 
 * 优先级：
 * - criterions（最高）：完全自定义的查询条件，会覆盖所有自动生成的条件
 * - operators：为特定属性指定操作符，覆盖默认的等于操作
 * - 默认规则：属性值非空时使用等于操作，属性间AND关系
 * 
 * 使用场景：
 * - RESTful API的查询参数封装
 * - 动态查询条件的构建
 * - 简化查询代码的编写
 * 
 * 注意事项：
 * - 子类需要定义查询属性，属性名对应数据库字段或实体属性
 * - returnEntityClass的属性可以比PO多，但只会自动封装名字一致的属性
 * - nullProperties用于处理需要查询null值的特殊场景
 * 
 * @since 1.0.0
 */
open class SearchPayload {

    /** 各属性间的查询逻辑关系，默认为AND */
    open var andOr: AndOrEnum = AndOrEnum.AND

    /** 值为null时须作为查询条件的属性的列表 */
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