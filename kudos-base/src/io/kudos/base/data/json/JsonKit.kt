package io.kudos.base.data.json

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONWriter
import com.alibaba.fastjson2.TypeReference
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import kotlin.reflect.KClass

/**
 * json工具类(基于fastjson)
 * 注意事项：
 * 1.支持数据类和普通类,必须有空构造函数，要映射的属性必须是可读可写的(var)
 *
 * @author K
 * @since 1.0.0
 */
object JsonKit {

    private val log = LogFactory.getLog(this)

    /**
     * 返回json串中指定属性名的属性值
     *
     * @param jsonStr 待解析的json串
     * @param propertyName 属性名
     * @return 属性值，如果找不到属性或出错，则返回null
     * @author K
     * @since 1.0.0
     */
    fun getPropertyValue(jsonStr: String, propertyName: String): Any? {
        return try {
            JSON.parseObject(jsonStr)[propertyName]
        } catch (e: Exception) {
            log.error(e)
            null
        }
    }

    /**
     * 将简单的Json串格式化成页面显示的字符串(去掉花括号、引号及最后面可能的逗号)
     *
     * @param simpleJsonStr 简单的Json串格式化(如：{"A":"b","B":'b'} ), 为空将返回空串
     * @return 页面显示的字符串(如：A:b, B:b)
     * @author K
     * @since 1.0.0
     */
    fun jsonToDisplay(simpleJsonStr: String): String {
        if (simpleJsonStr.isBlank()) {
            return ""
        }
        var displayStr = simpleJsonStr.replaceFirst("^\\{".toRegex(), "")
        displayStr = displayStr.replaceFirst("\\}$".toRegex(), "")
        displayStr = displayStr.replace("\"|'".toRegex(), "")
        displayStr = displayStr.replaceFirst(",$".toRegex(), "")
        return displayStr
    }

    /**
     * 反序列化, 将json串解析为指定Class的实例
     *
     * @param T 目标类型
     * @param json json串
     * @param clazz Class
     * @return Class的实例，出错时返回null
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> fromJson(json: String, clazz: KClass<T>): T? {
        return try {
            JSON.parseObject(json, clazz.java)
        } catch (e: Exception) {
            log.error(e)
            null
        }
    }

    /**
     * 反序列化, 将json串解析为指定TypeReference的实例
     *
     * @param T 目标类型
     * @param json json串
     * @param typeReference TypeReference子类，用来指定泛型参数
     * @return Class的实例，出错时返回null
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> fromJson(json: String, typeReference: TypeReference<T>): T? {
        return try {
            JSON.parseObject(json, typeReference)
        } catch (e: Exception) {
            log.error(e)
            null
        }
    }


    /**
     * 序列化，将对象转为json串
     *
     * @param obj 要序列化的对象，可以是一般对象，也可以是Collection或数组， 如果集合为空集合, 返回"[]"
     * @param preserveNull 是否保留null值，默认为否
     * @return 序列化后的json串，出错时返回空串
     * @author K
     * @since 1.0.0
     */
    fun toJson(obj: Any, preserveNull: Boolean = false): String {
        return try {
            if (preserveNull) {
                JSON.toJSONString(obj, JSONWriter.Feature.WriteMapNullValue)
            } else {
                JSON.toJSONString(obj)
            }
        } catch (e: Exception) {
            log.error(e)
            ""
        }
    }

    /**
     * 输出jsonP格式的数据
     *
     * @param functionName 函数名
     * @param obj 待序列化的对象，其json对象将作为函数的参数
     * @param preserveNull 是否保留null值，默认为否
     * @return jsonP字符串
     * @author K
     * @since 1.0.0
     */
    fun toJsonP(functionName: String, obj: Any, preserveNull: Boolean = false): String {
        return "${functionName}(${toJson(obj, preserveNull)})"
    }

    /**
     * 当json里含有bean的部分属性时，用json串中的值更新该bean的该部分属性
     *
     * @param T bean类型
     * @param jsonStr json串
     * @param obj 待更新的bean
     * @return 更新后的bean，失败时返回null
     * @author K
     * @since 1.0.0
     */
    fun <T: Any> updateBean(jsonStr: String, obj: T): T? {
        return try {
            val o = JSON.parseObject(jsonStr, obj::class.java)
            BeanKit.copyProperties(o, obj)
        } catch (e: Exception) {
            log.error(e)
            null
        }
    }

}