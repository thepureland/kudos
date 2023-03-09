package io.kudos.base.data.xml

import io.kudos.base.support.Consts
import org.soul.base.data.xml.XmlTool
import kotlin.reflect.KClass

/**
 * xml工具类(基于JAXB)
 * JAXB = Java Architecture for XML Binding
 * 使用Jaxb2.0实现XML和Java的相互转化, OXM(Object XML Mapping), JAXB2在底层是用StAX(JSR 173)来处理XML文档的。
 * 注意事项：
 * 1.支持数据类和普通类,必须有空构造函数，要映射的属性必须是可读可写的(var)
 *
 * @author K
 * @since 1.0.0
 */
object XmlKit {

    /**
     * 序列化(编组)，按指定编码将bean转为xml
     *
     * @param root 待序列化的根对象
     * @param encoding 编码名称,缺省为UTF-8
     * @return 序列化后的xml字符串
     * @author K
     * @since 1.0.0
     */
    fun toXml(root: Any, encoding: String = "UTF-8"): String {
        return XmlTool.toXml(root, encoding)
    }

    /**
     * 序列化(编组)，特别支持Root Element是Collection的情形.
     *
     * @param T 集合元素类型
     * @param root 待序列化的根容器对象
     * @param rootName 根的名称
     * @param clazz 类
     * @param encoding 编码名称,缺省为UTF-8
     * @return 序列化后的xml字符串
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> toXml(root: Collection<T>, rootName: String, clazz: KClass<T>, encoding: String = "UTF-8"): String {
        return XmlTool.toXml(root, rootName, clazz.java, encoding)
    }

    /**
     * 反序列化(解组)，将xml转为指定类的实例
     *
     * @param T 目标类型
     * @param xml xml字符串
     * @param clazz 实例的类型
     * @param ignoreNameSpace 是否忽略命名空间
     * @return 指定类的实例
     * @author K
     * @since 1.0.0
     */
    @Suppress(Consts.Suppress.UNCHECKED_CAST)
    fun <T : Any> fromXml(xml: String, clazz: KClass<T>, ignoreNameSpace: Boolean = false): T {
        return if (ignoreNameSpace)
            XmlTool.fromXml(xml, clazz.java)
        else XmlTool.fromXmlIgnoreNameSpace(xml, clazz.java)
    }

}