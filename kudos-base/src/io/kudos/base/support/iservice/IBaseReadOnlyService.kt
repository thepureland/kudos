package io.kudos.base.support.iservice

import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import io.kudos.base.support.IIdEntity
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.base.support.payload.SearchPayload
import kotlin.reflect.KClass

/**
 * 基础只读业务操作接口
 * 
 * 定义了业务层的只读操作，包括查询、分页、聚合计算等功能。
 * 基于关系型数据库表，提供统一的查询接口，简化业务层代码。
 * 
 * 核心功能：
 * 1. 主键查询：根据主键查询单个或批量实体
 * 2. 单属性查询：根据单个属性值查询（oneSearch）
 * 3. 全量查询：查询所有记录（allSearch）
 * 4. 多属性查询：支持AND和OR两种逻辑关系的多属性查询
 * 5. IN查询：支持IN条件查询，包括主键IN查询
 * 6. 复杂条件查询：使用Criteria对象构建复杂查询条件
 * 7. 分页查询：支持分页查询，可控制返回的属性
 * 8. 载体查询：使用ListSearchPayload载体对象进行查询
 * 9. 聚合计算：支持count、sum、avg、max、min等聚合函数
 * 
 * 查询方法分类：
 * - 基础查询：get、oneSearch、allSearch、andSearch、orSearch、inSearch
 * - 属性查询：每个基础查询都有对应的Property版本，返回单个属性值列表
 * - 多属性查询：每个基础查询都有对应的Properties版本，返回Map列表
 * - 复杂查询：search、pagingSearch、search(payload)
 * 
 * 结果类型：
 * - 实体列表：返回完整的实体对象列表（List<E>）
 * - 属性值列表：返回单个属性的值列表（List<*>）
 * - Map列表：返回多个属性的Map列表（List<Map<String, *>>）
 * - 聚合结果：返回Number或Any?类型的聚合计算结果
 * - 分页结果：返回Pair<List<*>, Int>，包含结果列表和总记录数
 * 
 * 排序支持：
 * - 所有查询方法都支持可变参数的Order排序
 * - 可以指定多个排序规则，按优先级执行
 * 
 * 与DAO层的关系：
 * - 业务层接口，通常委托给DAO层实现
 * - 可以在业务层添加业务逻辑处理
 * - 提供统一的业务接口，隐藏DAO层细节
 * 
 * 使用场景：
 * - 业务层的查询操作
 * - RESTful API的服务层
 * - 报表和统计功能
 * 
 * 注意事项：
 * - 所有查询方法都是只读操作，不会修改数据
 * - 主键类型支持String、Int、Long
 * - 批量查询支持分批处理，避免内存溢出
 * - 分页查询的页码从1开始
 * 
 * @param PK 实体主键类型
 * @param E 实体类型，必须实现IIdEntity接口
 * @since 1.0.0
 */
interface IBaseReadOnlyService<PK : Any, E : IIdEntity<PK>> {

    //region by id

    /**
     * 查询指定主键值的实体
     *
     * @param id 主键值，类型必须为以下之一：String、Int、Long
     * @return 实体，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun get(id: PK): E?

    /**
     * 查询指定主键值的实体，可以指定返回的对象类型
     *
     * @param id 主键值，类型必须为以下之一：String、Int、Long
     * @param returnType 返回对象的类型
     * @return 结果对象，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun <R : Any> getAs(id: PK, returnType: KClass<R>): R?

    /**
     * 批量查询指定主键值的实体
     *
     * @param ids 主键值可变参数，元素类型必须为以下之一：String、Int、Long，为空时返回空列表
     * @param countOfEachBatch 每批大小，缺省为1000
     * @return 实体列表，ids为空时返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getByIds(vararg ids: PK, countOfEachBatch: Int = 1000): List<E>

    //endregion by id


    //region oneSearch
    /**
     * 根据单个属性查询
     *
     * @param property 属性名
     * @param value    属性值
     * @param orders   排序规则
     * @return 指定类名对象的结果列表
     * @author K
     * @since 1.0.0
     */
    fun oneSearch(property: String, value: Any?, vararg orders: Order): List<E>

    /**
     * 根据单个属性查询，只返回指定的单个属性的列表
     *
     * @param property       属性名
     * @param value          属性值
     * @param returnProperty 返回的属性名
     * @param orders         排序规则
     * @return List(属性值)
     * @author K
     * @since 1.0.0
     */
    fun oneSearchProperty(property: String, value: Any?, returnProperty: String, vararg orders: Order): List<*>

    /**
     * 根据单个属性查询，只返回指定属性的列表
     *
     * @param property         属性名
     * @param value            属性值
     * @param returnProperties 返回的属性名称集合
     * @param orders           排序规则
     * @return List(Map(属性名, 属性值)), 一个Map封装一个记录
     * @author K
     * @since 1.0.0
     */
    fun oneSearchProperties(
        property: String, value: Any?, returnProperties: Collection<String>, vararg orders: Order
    ): List<Map<String, *>>

    //endregion oneSearch


    //region allSearch
    /**
     * 查询所有结果
     *
     * @param orders 排序规则
     * @return 实体对象列表
     * @author K
     * @since 1.0.0
     */
    fun allSearch(vararg orders: Order): List<E>

    /**
     * 查询所有结果，只返回指定的单个属性的列表
     *
     * @param returnProperty 属性名
     * @param orders         排序规则
     * @return List(属性值)
     * @author K
     * @since 1.0.0
     */
    fun allSearchProperty(returnProperty: String, vararg orders: Order): List<*>

    /**
     * 查询所有结果，只返回指定属性的列表
     *
     * @param returnProperties 属性名称集合
     * @param orders           排序规则
     * @return List(Map(属性名, 属性值))
     * @author K
     * @since 1.0.0
     */
    fun allSearchProperties(returnProperties: Collection<String>, vararg orders: Order): List<Map<String, *>>

    //endregion allSearch


    //region andSearch
    /**
     * 根据多个属性进行and条件查询，返回实体类对象的列表
     *
     * @param properties Map(属性名，属性值)
     * @param orders     排序规则
     * @return 实体对象列表
     * @author K
     * @since 1.0.0
     */
    fun andSearch(properties: Map<String, *>, vararg orders: Order): List<E>

    /**
     * 根据多个属性进行and条件查询，只返回指定的单个属性的列表
     *
     * @param properties     Map(属性名，属性值）
     * @param returnProperty 要返回的属性名
     * @param orders         排序规则
     * @return List(指定的属性的值)
     * @author K
     * @since 1.0.0
     */
    fun andSearchProperty(properties: Map<String, *>, returnProperty: String, vararg orders: Order): List<*>

    /**
     * 根据多个属性进行and条件查询，只返回指定属性的列表
     *
     * @param properties       Map(属性名，属性值)
     * @param returnProperties 要返回的属性名集合
     * @param orders           排序规则
     * @return List(Map(指定的属性名，属性值))
     * @author K
     * @since 1.0.0
     */
    fun andSearchProperties(
        properties: Map<String, *>, returnProperties: Collection<String>, vararg orders: Order
    ): List<Map<String, *>>

    //endregion andSearch


    //region orSearch
    /**
     * 根据多个属性进行or条件查询，返回实体类对象的列表
     *
     * @param properties Map(属性名，属性值)
     * @param orders     排序规则
     * @return 实体对象列表
     * @author K
     * @since 1.0.0
     */
    fun orSearch(properties: Map<String, *>, vararg orders: Order): List<E>

    /**
     * 根据多个属性进行or条件查询，只返回指定的单个属性的列表
     *
     * @param properties     Map(属性名，属性值)
     * @param returnProperty 要返回的属性名
     * @param orders         排序规则
     * @return List(指定的属性的值)
     * @author K
     * @since 1.0.0
     */
    fun orSearchProperty(properties: Map<String, *>, returnProperty: String, vararg orders: Order): List<*>

    /**
     * 根据多个属性进行or条件查询，只返回指定的属性的列表
     *
     * @param properties       Map(属性名，属性值)
     * @param returnProperties 要返回的属性名集合
     * @param orders           排序规则
     * @return List(Map(指定的属性名，属性值))
     * @author K
     * @since 1.0.0
     */
    fun orSearchProperties(
        properties: Map<String, *>, returnProperties: Collection<String>, vararg orders: Order,
    ): List<Map<String, *>>

    //endregion orSearch


    //region inSearch

    /**
     * in查询，返回实体类对象列表
     *
     * @param property 属性名
     * @param values   in条件值集合
     * @param orders   排序规则
     * @return 指定类名对象的结果列表
     * @author K
     * @since 1.0.0
     */
    fun inSearch(property: String, values: Collection<*>, vararg orders: Order): List<E>

    /**
     * in查询，只返回指定的单个属性的值
     *
     * @param property       属性名
     * @param values         in条件值集合
     * @param returnProperty 要返回的属性名
     * @param orders         排序规则
     * @return 指定属性的值列表
     * @author K
     * @since 1.0.0
     */
    fun inSearchProperty(
        property: String, values: Collection<*>, returnProperty: String, vararg orders: Order
    ): List<*>

    /**
     * in查询，只返回指定属性的值
     *
     * @param property         属性名
     * @param values           in条件值集合
     * @param returnProperties 要返回的属性名集合
     * @param orders           排序规则
     * @return List(Map(指定的属性名，属性值))
     * @author K
     * @since 1.0.0
     */
    fun inSearchProperties(
        property: String, values: Collection<*>, returnProperties: Collection<String>, vararg orders: Order
    ): List<Map<String, *>>

    /**
     * 主键in查询，返回实体类对象列表
     *
     * @param values 主键值集合
     * @param orders 排序规则
     * @param orders 排序规则
     * @return 实体对象列表
     * @author K
     * @since 1.0.0
     */
    fun inSearchById(values: Collection<PK>, vararg orders: Order): List<E>

    /**
     * 主键in查询，只返回指定的单个属性的值
     *
     * @param values         主键值集合
     * @param returnProperty 要返回的属性名
     * @param orders         排序规则
     * @return 指定属性的值列表
     * @author K
     * @since 1.0.0
     */
    fun inSearchPropertyById(values: Collection<PK>, returnProperty: String, vararg orders: Order): List<*>

    /**
     * 主键in查询，只返回指定属性的值
     *
     * @param values           主键值集合
     * @param returnProperties 要返回的属性名集合
     * @param orders           排序规则
     * @return List(Map(指定的属性名, 属性值))
     * @author K
     * @since 1.0.0
     */
    fun inSearchPropertiesById(
        values: Collection<PK>, returnProperties: Collection<String>, vararg orders: Order
    ): List<Map<String, *>>

    //endregion inSearch


    //region search Criteria
    /**
     * 复杂条件查询
     *
     * @param criteria 查询条件
     * @param orders   排序规则
     * @return 实体对象列表
     * @author K
     * @since 1.0.0
     */
    fun search(criteria: Criteria, vararg orders: Order): List<E>

    /**
     * 复杂条件查询，可以指定返回的封装类。会忽略与表实体不匹配的属性。
     *
     * 该方法的目的主要是为了避免各应用场景下，需要将PO转为所需VO的麻烦与性能开销。
     *
     * @param T 返回列表项的类型
     * @param criteria 查询条件，为null表示无条件查询，缺省为null
     * @param returnItemClass 返回项的类型, 为null时按表的实体类处理
     * @param orders   排序规则
     * @return 指定的返回类型对象列表
     * @author K
     * @since 1.0.0
     */
    fun <T: Any> search(criteria: Criteria? = null, returnItemClass: KClass<T>? = null, vararg orders: Order): List<T>

    /**
     * 复杂条件查询，只返回指定单个属性的值
     *
     * @param criteria       查询条件
     * @param returnProperty 要返回的属性名
     * @param orders         排序规则
     * @return 指定属性的值列表
     * @author K
     * @since 1.0.0
     */
    fun searchProperty(criteria: Criteria, returnProperty: String, vararg orders: Order): List<*>

    /**
     * 复杂条件，只返回指定多个属性的值
     *
     * @param criteria         查询条件
     * @param returnProperties 要返回的属性名集合
     * @param orders           排序规则
     * @return List(Map(指定的属性名 属性值))
     * @author K
     * @since 1.0.0
     */
    fun searchProperties(
        criteria: Criteria, returnProperties: Collection<String>, vararg orders: Order
    ): List<Map<String, Any?>>

    //endregion search Criteria


    //region pagingSearch
    /**
     * 分页查询，返回对象列表
     *
     * @param criteria 查询条件
     * @param pageNo   当前页码(从1开始)
     * @param pageSize 每页条数
     * @param orders   排序规则
     * @return 实体对象列表
     * @author K
     * @since 1.0.0
     */
    fun pagingSearch(criteria: Criteria?, pageNo: Int, pageSize: Int, vararg orders: Order): List<E>

    /**
     * 分页查询，可以指定返回的封装类。会忽略与表实体不匹配的属性。
     *
     * 该方法的目的主要是为了避免各应用场景下，需要将PO转为所需VO的麻烦与性能开销。
     *
     * @param T 返回列表项的类型
     * @param criteria 查询条件，为null表示无条件查询，缺省为null
     * @param returnItemClass 返回项的类型, 为null时按表的实体类处理
     * @param pageNo   当前页码(从1开始)
     * @param pageSize 每页条数
     * @param orders   排序规则
     * @return 指定的返回类型对象列表
     * @author K
     * @since 1.0.0
     */
    fun <T: Any> pagingSearch(
        criteria: Criteria? = null,
        returnItemClass: KClass<T>? = null,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<T>

    /**
     * 分页查询，返回对象列表,返回只包含指定属性
     *
     * @param criteria 查询条件
     * @param pageNo   当前页码(从1开始)
     * @param pageSize 每页条数
     * @param orders   排序规则
     * @return 实体对象列表
     * @author K
     * @since 1.0.0
     */
    fun pagingReturnProperty(
        criteria: Criteria, returnProperty: String, pageNo: Int, pageSize: Int, vararg orders: Order
    ): List<*>

    /**
     * 分页查询，返回对象列表,返回只包含指定属性
     *
     * @param criteria 查询条件
     * @param pageNo   当前页码(从1开始)
     * @param pageSize 每页条数
     * @param orders   排序规则
     * @return 实体对象列表
     * @author K
     * @since 1.0.0
     */
    fun pagingReturnProperties(
        criteria: Criteria, returnProperties: Collection<String>, pageNo: Int, pageSize: Int, vararg orders: Order
    ): List<Map<String, *>>

    /**
     * 根据查询载体对象分页查询，返回查询结果及总记录数
     *
     * @param listSearchPayload 查询载体对象
     * @return Pair(List(结果对象), 总记录数)， 结果对象有三种类型可能, @see SearchPayload
     * @author K
     * @since 1.0.0
     */
    fun pagingSearch(listSearchPayload: ListSearchPayload): Pair<List<*>, Int>

    //endregion pagingSearch


    //region payload search

    /**
     * 根据查询载体对象查询(包括分页), 具体规则见 @see SearchPayload
     *
     * @param listSearchPayload 查询载体对象
     * @return 结果列表, 有三种类型可能, @see SearchPayload
     * @author K
     * @since 1.0.0
     */
    fun search(listSearchPayload: ListSearchPayload): List<*>

    //endregion payload search


    //region aggregate

    /**
     * 计算记录数
     *
     * @param criteria 查询条件，为null将计算所有记录
     * @return 记录数
     * @author K
     * @since 1.0.0
     */
    fun count(criteria: Criteria? = null): Int

    /**
     * 计算记录数
     *
     * @param searchPayload 查询载体对象
     * @return 记录数
     * @author K
     * @since 1.0.0
     */
    fun count(searchPayload: SearchPayload): Int

    /**
     * 求和. 对满足条件的记录根据指定属性进行求和
     *
     * @param property 待求和的属性
     * @param criteria 查询条件，为null将计算所有记录
     * @return 和
     * @author K
     * @since 1.0.0
     */
    fun sum(property: String, criteria: Criteria? = null): Number

    /**
     * 求平均值. 对满足条件的记录根据指定属性进行求平均值
     *
     * @param property 待求平均值的属性
     * @param criteria 查询条件，为null将计算所有记录
     * @return 平均值
     * @author K
     * @since 1.0.0
     */
    fun avg(property: String, criteria: Criteria? = null): Number

    /**
     * 求最大值. 对满足条件的记录根据指定属性进行求最大值
     *
     * @param property 待求最大值的属性
     * @param criteria 查询条件，为null将计算所有记录
     * @return 最大值
     * @author K
     * @since 1.0.0
     */
    fun max(property: String, criteria: Criteria? = null): Any?

    /**
     * 求最小值. 对满足条件的记录根据指定属性进行求最小值
     *
     * @param property 待求最小值的属性
     * @param criteria 查询条件，为null将计算所有记录
     * @return 最小值
     * @author K
     * @since 1.0.0
     */
    fun min(property: String, criteria: Criteria? = null): Any?

    //endregion aggregate

}