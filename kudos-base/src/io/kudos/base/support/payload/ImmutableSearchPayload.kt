package io.kudos.base.support.payload

import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.logic.AndOrEnum
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0


/**
 * 不可变查询条件载体（父类）
 *
 * 用于封装查询条件，支持灵活的查询规则配置和结果类型控制。
 * 子类通过**在类中明确定义属性**来自动生成查询条件，简化查询代码的编写。
 *
 * **安全性设计**：本类中与查询相关的属性均声明为 [val]，只能在子类定义时赋初值，
 * 无法在实例创建后由外部修改。因此适合承载来自不信任源（如前端、开放 API）的查询参数，
 * 避免调用方在构造后篡改条件，从而降低注入与越权风险。同时也不应该在子类中将val修改为var!
 *
 * 核心功能：
 * 1. 自动条件生成：根据属性值自动生成查询条件
 * 2. 灵活操作符：支持为每个属性指定不同的操作符（子类通过重写 [operators] 等赋初值）
 * 3. 逻辑关系控制：支持 AND 和 OR 两种逻辑关系
 * 4. 结果类型控制：支持返回实体、属性值或 Map 等多种结果类型
 * 5. 自定义条件：支持完全自定义的查询条件
 *
 * 查询规则：
 * 1. 空值过滤：各个属性只有当其值非空时才会应用该查询条件
 *    - 例外：[nullProperties] 中指定的属性，即使值为 null 也会作为查询条件
 * 2. 默认操作符：各个属性的查询逻辑默认都是等于（EQ）
 *    - 可通过 [operators] 为特定属性指定不同的操作符
 * 3. 逻辑关系：各属性间的关系默认为 AND
 *    - 可通过重写 [andOr] 改变为 OR
 *
 * 结果类型控制：
 * - [returnProperties] 为空：返回 [returnEntityClass] 指定的实体列表，或表对应的 PO 列表
 * - [returnProperties] 为单个属性：返回该属性值的列表（如 List<String>）
 * - [returnProperties] 为多个属性：返回 Map(属性名, 属性值) 的列表
 *
 * 优先级：
 * - [criterions]（最高）：完全自定义的查询条件，会覆盖所有自动生成的条件
 * - [operators]：为特定属性指定操作符，覆盖默认的等于操作
 * - 默认规则：属性值非空时使用等于操作，属性间 AND 关系
 *
 * 使用场景：
 * - RESTful API 的查询参数封装（推荐：由子类固定结构，请求只填充属性值）
 * - 需要防止外部篡改查询条件的场景
 *
 * 注意事项：
 * - 子类需要定义查询属性，属性名对应数据库字段或实体属性
 * - [returnEntityClass] 的属性可以比 PO 多，但只会自动封装名字一致的属性
 * - [nullProperties] 用于处理需要查询 null 值的特殊场景
 *
 * @author K
 * @since 1.0.0
 */
open class ImmutableSearchPayload {

    /** 各属性间的查询逻辑关系，默认为AND */
    open val andOr: AndOrEnum = AndOrEnum.AND

    /** 值为null时须作为查询条件的属性的列表 */
    open val nullProperties: List<String>? = null

    /**
     * 属性的特殊(非等于)查询操作逻辑，key必须为该类的属性名
     */
    open val operators: Map<KProperty0<*>, OperatorEnum>? = null

    /**
     * 返回的实体类型
     * 仅当 returnProperties 为空时才会应用,
     * 如果此时 returnEntityClass 为null, 将返回所查询表对应的PO.
     * 该类中定义的属性可以比PO的多,但是只会自动封装名字一致的(类型要能兼容).
     */
    open val returnEntityClass: KClass<*>? = null

    /**
     * 查询结果的属性列表
     * 查询结果类型:
     *    如果为空, 则为指定的returnEntityClass对象列表或所查询表对应的PO列表;
     *    如果单个属性, 则为该属性值的列表;
     *    如果多个属性, 则为Map(属性名, 属性值)的列表;
     */
    open val returnProperties: List<String>? = null

    /**
     * 完全自定义的属性查询逻辑。优先级最高，会覆盖原来的查询逻辑！
     */
    open val criterions: List<Criterion>? = null

}