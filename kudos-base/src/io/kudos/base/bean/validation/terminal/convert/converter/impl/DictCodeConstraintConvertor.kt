package io.kudos.base.bean.validation.terminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.bean.validation.terminal.convert.converter.IDictItemCodeFinder
import java.util.LinkedHashMap
import java.util.ServiceLoader

/**
 * 字典码约束转换器
 * 用于将字典码注解转换为前端校验规则，支持动态获取字典数据并生成校验规则
 */
class DictCodeConstraintConvertor(annotation: Annotation) : DefaultConstraintConvertor(annotation) {

    override fun getRule(constraintAnnotation: Annotation): LinkedHashMap<String, Any> {
        val map = super.getRule(constraintAnnotation)
        require(constraintAnnotation is DictItemCode) { "DictCodeConstraintConvertor 仅支持 DictCode 注解" }
        val dictCode = constraintAnnotation
        val codes = dictCodeConvertor(dictCode.atomicServiceCode, dictCode.dictType)
        map["values"] = codes
        return map
    }

    /**
     * 通过 [ServiceLoader] SPI 加载 [IDictItemCodeFinder] 取首个实现的字典 code 集合。
     * 与 [DictItemCodeValidator.dictCodeConvertor] 同理走 SPI 而非 Spring bean——
     * 本类作为 bean validation 的 constraint converter，由框架反射 new 出来。
     *
     * @param module 原子服务编码
     * @param dictType 字典类型
     * @return 字典 code 集合；无实现时返回空集
     * @author K
     * @since 1.0.0
     */
    private fun dictCodeConvertor(module: String, dictType: String): Set<String> =
        ServiceLoader.load(IDictItemCodeFinder::class.java).firstOrNull()
            ?.getDictItemCodes(module, dictType) ?: emptySet()
}
