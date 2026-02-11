package io.kudos.base.query

import io.kudos.base.query.enums.OperatorEnum
import java.io.Serializable
import kotlin.reflect.KProperty1

/**
 * 查询条件封装类
 * 
 * 用于封装多个查询条件（WHERE子句），支持AND、OR逻辑的任意组合和嵌套。
 * 
 * 核心功能：
 * 1. 条件组合：支持多个查询条件的AND和OR组合
 * 2. 嵌套查询：支持Criteria的嵌套，实现复杂的查询逻辑
 * 3. 条件过滤：自动过滤空值和无效条件
 * 4. 链式调用：提供流畅的API，支持链式调用
 * 
 * 数据结构：
 * - criterionGroups：存储所有查询条件组
 *   - Criterion：单个查询条件（属性、操作符、值）
 *   - Criteria：嵌套查询对象（AND关系）
 *   - Array<*>：OR组（数组内元素是OR关系，数组与其他元素是AND关系）
 * 
 * 逻辑关系：
 * - criterionGroups中的元素之间是AND关系
 * - Array<*>中的元素之间是OR关系
 * - 支持任意层级的嵌套
 * 
 * 条件过滤：
 * - null值：只有操作符acceptNull为true时才添加
 * - 空字符串：不添加（除非操作符acceptNull）
 * - 空集合/数组：不添加
 * - 空的嵌套Criteria：不添加
 * 
 * 使用场景：
 * - 动态构建查询条件
 * - 复杂查询逻辑的封装
 * - ORM框架的查询构建
 * 
 * 注意事项：
 * - 条件值会被自动过滤，避免生成无效的查询
 * - toString方法仅用于调试，不能直接作为SQL执行
 * - 支持静态工厂方法创建Criteria对象
 * 
 * @since 1.0.0
 */
class Criteria : Serializable {

    /**
     * 列表各个元素均为and关系，元素类型如下： <br></br>
     * 1. Criterion
     * 2. Criteria
     * 3. 数组，元素类型可能为Criterion或Criteria， 数组各个元素是or关系
     */
    private val criterionGroups = mutableListOf<Any>()

    constructor()

    constructor(property: String, operatorEnum: OperatorEnum, value: Any?) {
        addAnd(property, operatorEnum, value)
    }

    /**
     * 封装单个查询条件
     *
     * @param criterion
     */
    constructor(criterion: Criterion) {
        addAnd(criterion)
    }


    //region and
    /**
     * 添加单个查询条件
     *
     * @param property 属性名
     * @param operatorEnum 逻辑操作符枚举
     * @param value    属性值
     * @return 当前查询对象
     */
    fun addAnd(property: String, operatorEnum: OperatorEnum, value: Any?): Criteria {
        return addAnd(Criterion(property, operatorEnum, value))
    }

    /**
     * 添加多个查询条件，之间是与的关系
     *
     * @param criterions 查询条件可变参数
     * @return 当前查询对象
     */
    fun addAnd(vararg criterions: Criterion): Criteria {
        if (criterions.isNotEmpty()) {
            addCriterion(criterionGroups, *criterions)
        }
        return this
    }

    /**
     * 添加多个查询对象(嵌套)，之间是与的关系
     *
     * @param criterias 查询对象可变参数
     * @return 当前查询对象
     */
    fun addAnd(vararg criterias: Criteria): Criteria {
        if (criterias.isNotEmpty()) {
            addCriteria(criterionGroups, *criterias)
        }
        return this
    }

    /**
     * 添加一个查询条件和一个查询对象(嵌套)，之间是与的关系
     *
     * @param criterion 查询条件
     * @param criteria  查询对象(嵌套)
     * @return 当前查询对象
     */
    fun addAnd(criterion: Criterion, criteria: Criteria): Criteria {
        return addAnd(criterion).addAnd(criteria)
    }

    /**
     * 添加一个查询对象(嵌套)和一个查询条件，之间是与的关系
     *
     * @param criteria  查询对象(嵌套)
     * @param criterion 查询条件
     * @return 当前查询对象
     */
    fun addAnd(criteria: Criteria, criterion: Criterion): Criteria {
        return addAnd(criteria).addAnd(criterion)
    }
    //endregion and

    //region or
    /**
     * 添加多个查询条件，之间是或的关系
     *
     * @param criterions 查询条件可变参数
     * @return 当前查询对象
     */
    fun addOr(vararg criterions: Criterion): Criteria {
        if (criterions.isNotEmpty()) {
            addOrGroup(addCriterion(null, *criterions))
        }
        return this
    }

    /**
     * 添加多个查询对象(嵌套)，之间是或的关系
     *
     * @param criterias 查询对象可变参数
     * @return 当前查询对象
     */
    fun addOr(vararg criterias: Criteria): Criteria {
        if (criterias.isNotEmpty()) {
            addOrGroup(addCriteria(null, *criterias))
        }
        return this
    }

    /**
     * 添加一个查询条件和一个查询对象(嵌套)，之间是或的关系
     *
     * @param criterion 查询条件
     * @param criteria  查询对象(嵌套)
     * @return 当前查询对象
     */
    fun addOr(criterion: Criterion, criteria: Criteria): Criteria {
        val objList = addCriterion(null, criterion)
        addCriteria(objList, criteria)
        addOrGroup(objList)
        return this
    }

    /**
     * 添加一个查询对象(嵌套)和一个查询条件，之间是或的关系
     *
     * @param criteria  查询对象(嵌套)
     * @param criterion 查询条件
     * @return 当前查询对象
     */
    fun addOr(criteria: Criteria, criterion: Criterion): Criteria {
        val objList = addCriteria(null, criteria)
        addCriterion(objList, criterion)
        addOrGroup(objList)
        return this
    }

    //endregion or

    /**
     * 是否条件为空
     *
     * @return true：查询条件为空， 反之不为空
     * @author K
     * @since 1.0.0
     */
    fun isEmpty(): Boolean {
        return criterionGroups.isEmpty()
    }

    /**
     * 添加查询条件到列表
     * 
     * 将查询条件添加到指定列表，并根据条件值进行过滤。
     * 
     * 工作流程：
     * 1. 创建或使用列表：如果list为null，创建新列表；否则使用现有列表
     * 2. 过滤条件：遍历所有条件，根据值是否有效决定是否添加
     * 3. 值有效性判断：
     *    - 非空且非空字符串：有效
     *    - 集合非空：有效
     *    - 数组非空：有效
     *    - 操作符接受null：有效（即使值为null）
     * 4. 添加到列表：将有效条件添加到列表
     * 
     * 值过滤规则：
     * - null值：只有操作符acceptNull为true时才添加
     * - 空字符串：不添加（除非操作符acceptNull）
     * - 空集合/数组：不添加
     * - 非空值：添加
     * 
     * 使用场景：
     * - 添加AND条件：list为criterionGroups
     * - 添加OR条件：list为null，创建临时列表
     * 
     * @param list 目标列表，如果为null则创建新列表
     * @param criterions 待添加的查询条件
     * @return 添加条件后的列表
     */
    private fun addCriterion(list: MutableList<Any>?, vararg criterions: Criterion): MutableList<Any> {
        var resultList = list
        if (resultList == null) {
            resultList = ArrayList(criterions.size)
        }
        for (criterion in criterions) {
            val operator = criterion.operator
            val value = criterion.value
            if (value != null && value !is Collection<*> && value !is Array<*> && "" != value || value is Collection<*> && !value.isEmpty()
                || value is Array<*> && value.isNotEmpty() || operator.acceptNull
            ) {
                resultList.add(criterion)
            }
        }
        return resultList
    }

    /**
     * 添加查询对象到列表
     * 
     * 将查询对象（嵌套Criteria）添加到指定列表，只添加非空的查询对象。
     * 
     * 工作流程：
     * 1. 创建或使用列表：如果list为null，创建新列表；否则使用现有列表
     * 2. 过滤查询对象：遍历所有查询对象，只添加非空的（有条件的）
     * 3. 非空判断：检查查询对象的criterionGroups是否为空
     * 4. 添加到列表：将非空查询对象添加到列表
     * 
     * 非空判断：
     * - 如果查询对象的criterionGroups为空，不添加
     * - 避免添加空的嵌套查询，保持查询条件的简洁
     * 
     * 使用场景：
     * - 添加AND嵌套查询：list为criterionGroups
     * - 添加OR嵌套查询：list为null，创建临时列表
     * 
     * @param list 目标列表，如果为null则创建新列表
     * @param criterias 待添加的查询对象
     * @return 添加查询对象后的列表
     */
    private fun addCriteria(list: MutableList<Any>?, vararg criterias: Criteria): MutableList<Any> {
        var resultList = list
        if (resultList == null) {
            resultList = ArrayList(criterias.size)
        }
        for (criteria in criterias) {
            if (criteria.getCriterionGroups().isNotEmpty()) {
                resultList.add(criteria)
            }
        }
        return resultList
    }

    /**
     * 添加OR组到查询条件组
     * 
     * 将OR关系的条件列表转换为数组并添加到criterionGroups。
     * 
     * 工作流程：
     * 1. 检查列表：如果列表为空，不添加
     * 2. 转换为数组：将列表转换为数组（Array类型表示OR关系）
     * 3. 添加到组：将数组添加到criterionGroups
     * 
     * OR组标识：
     * - criterionGroups中的Array类型元素表示OR关系
     * - 数组中的元素之间是OR关系
     * - 数组与其他元素之间是AND关系
     * 
     * 数据结构：
     * - criterionGroups: List<Any>
     *   - Criterion: 单个条件
     *   - Criteria: 嵌套查询（AND关系）
     *   - Array<*>: OR组（数组内元素是OR关系）
     * 
     * @param list OR关系的条件列表
     */
    private fun addOrGroup(list: List<*>) {
        if (list.isNotEmpty()) {
            criterionGroups.add(list.toTypedArray())
        }
    }

    fun getCriterionGroups(): List<Any> {
        return criterionGroups
    }

    /**
     * 输出查询条件 <br></br>
     * 注: 输入内容只作查询条件关系的确认,不能当作实际执行sql语句!
     *
     * @return 多个查询条件字符串
     */
    override fun toString(): String {
        val sb = StringBuilder()
        if (criterionGroups.isNotEmpty()) {
            for (group in criterionGroups) {
                if (group is Criterion) {
                    sb.append(group.toString())
                } else if (group is Array<*>) { // or
                    if (group.isNotEmpty()) {
                        sb.append("(")
                        for (obj in group) {
                            sb.append(obj.toString())
                            sb.append(" OR ")
                        }
                        sb.delete(sb.length - 4, sb.length)
                        sb.append(")")
                    }
                } else if (group is Criteria) {
                    sb.append(group.toString())
                }
                sb.append(" AND ")
            }
            sb.delete(sb.length - 5, sb.length)
        }
        return sb.toString()
    }


    companion object {

        /**
         * 添加单个查询条件
         *
         * @param property 属性名
         * @param operatorEnum 逻辑操作符枚举
         * @param value    属性值
         * @return 新的查询对象
         */
        fun of(property: String, operatorEnum: OperatorEnum, value: Any?): Criteria =
            Criteria(Criterion(property, operatorEnum, value))

        //region static and

        /**
         * 添加多个查询条件，之间是与的关系
         *
         * @param criterions 查询条件可变参数
         * @return 新的查询对象
         */
        fun and(vararg criterions: Criterion): Criteria = Criteria().addAnd(*criterions)

        /**
         * 添加多个查询对象(嵌套)，之间是与的关系
         *
         * @param criterias 查询对象可变参数
         * @return 新的查询对象
         */
        fun and(vararg criterias: Criteria): Criteria = Criteria().addAnd(*criterias)

        /**
         * 添加一个查询条件和一个查询对象(嵌套)，之间是与的关系
         *
         * @param criterion 查询条件
         * @param criteria  查询对象(嵌套)
         * @return 新的查询对象
         */
        fun and(criterion: Criterion, criteria: Criteria): Criteria = Criteria().addAnd(criterion, criteria)

        /**
         * 添加一个查询对象(嵌套)和一个查询条件，之间是与的关系
         *
         * @param criteria  查询对象(嵌套)
         * @param criterion 查询条件
         * @return 新的查询对象
         */
        fun and(criteria: Criteria, criterion: Criterion): Criteria = Criteria().addAnd(criteria, criterion)

        //endregion static and

        //region static or
        /**
         * 添加多个查询条件，之间是或的关系
         *
         * @param criterions 查询条件可变参数
         * @return 新的查询对象
         */
        fun or(vararg criterions: Criterion): Criteria = Criteria().addOr(*criterions)

        /**
         * 添加多个查询对象(嵌套)，之间是或的关系
         *
         * @param criterias 查询对象可变参数
         * @return 新的查询对象
         */
        fun or(vararg criterias: Criteria): Criteria = Criteria().addOr(*criterias)

        /**
         * 添加一个查询条件和一个查询对象(嵌套)，之间是或的关系
         *
         * @param criterion 查询条件
         * @param criteria  查询对象(嵌套)
         * @return 新的查询对象
         */
        fun or(criterion: Criterion, criteria: Criteria): Criteria = Criteria().addOr(criterion, criteria)

        /**
         * 添加一个查询对象(嵌套)和一个查询条件，之间是或的关系
         *
         * @param criteria  查询对象(嵌套)
         * @param criterion 查询条件
         * @return 新的查询对象
         */
        fun or(criteria: Criteria, criterion: Criterion): Criteria = Criteria().addOr(criteria, criterion)
        //endregion static or
    }



}