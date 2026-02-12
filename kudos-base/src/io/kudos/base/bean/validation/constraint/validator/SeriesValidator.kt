package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.Series
import io.kudos.base.bean.validation.support.SeriesTypeEnum
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.math.BigDecimal

/**
 * Series约束验证器
 *
 * @author K
 * @since 1.0.0
 */
class SeriesValidator : ConstraintValidator<Series, Any?> {

    private lateinit var series: Series

    override fun initialize(series: Series) {
        this.series = series
        if (series.step < 0.0) {
            error("@Series约束注解的step不能为负数！")
        }
        if (series.size < 0) {
            error("@Series约束注解的size不能为负数！")
        }
    }

    /**
     * 验证值是否符合序列约束
     * 
     * 验证数组或列表中的元素是否符合指定的序列规则（递增、递减、等差等）。
     * 
     * 工作流程：
     * 1. null值检查：如果值为null，直接返回true（null值由@NotNull等注解处理）
     * 2. 类型检查：必须是Array或List类型，否则抛出异常
     * 3. 长度检查：如果元素数量<=1，直接返回true（单个元素无需验证序列）
     * 4. 大小检查：如果配置了size且实际大小不匹配，返回false
     * 5. null元素检查：数组中不能包含null元素，否则抛出异常
     * 6. 数值转换：将所有元素转换为BigDecimal进行高精度计算
     * 7. 序列验证：调用validate方法验证序列规则
     * 
     * 注意事项：
     * - 使用BigDecimal进行高精度计算，避免浮点数精度问题
     * - 数组元素会先转换为String再转为BigDecimal，确保类型兼容
     * - 只支持Array和List类型，其他类型会抛出异常
     * 
     * @param value 待验证的值，必须是Array或List类型
     * @param context 验证上下文
     * @return true表示验证通过，false表示验证失败
     */
    override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean {
        if (value == null) {
            return true
        }
        if (value !is Array<*> && value !is List<*>) {
            return fail(context, "@Series约束注解只能设置在返回值类型为Array或List的get方法上！")
        }
        var values = when (value) {
            is Array<*> -> value.toList()
            is List<*> -> value
            else -> emptyList()
        }
        if (values.size <= 1) {
            return true
        }
        if (series.size != 0 && values.size != series.size) {
            return false
        }
        if (values.any { it == null }) {
            return fail(context, "@Series约束注解限制数组中每个元素均不能为null！数组为：$value")
        }

        return try {
            // 将数组元素全部转为String，方便用BigDecimal进行高精度运算
            values = values.map { BigDecimal(it.toString()) }
            validate(series.type, series.step, *values.toTypedArray())
        } catch (_: RuntimeException) {
            fail(context, "@Series约束注解仅支持可转为数值的元素类型，数组为：$value")
        }
    }

    private fun fail(context: ConstraintValidatorContext, message: String): Boolean {
        context.disableDefaultConstraintViolation()
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation()
        return false
    }

    /**
     * 验证序列规则
     * 
     * 根据序列类型验证数值序列是否符合规则，支持多种序列模式。
     * 
     * 支持的序列类型：
     * - INC_DIFF：严格递增，相邻元素差值等于step（step=0时只需递增）
     * - DESC_DIFF：严格递减，相邻元素差值等于step（step=0时只需递减）
     * - INC_DIFF_DESC_DIFF：先递增后递减，找到最大值后验证前后两部分
     * - DESC_DIFF_INC_DIFF：先递减后递增，找到最小值后验证前后两部分
     * - DIFF：所有元素互不相同，且相邻元素差值等于step（step=0时只需互不相同）
     * - INC_EQ：非严格递增，允许相等，相邻元素差值等于step或0（step=0时只需非递减）
     * - DESC_EQ：非严格递减，允许相等，相邻元素差值等于-step或0（step=0时只需非递增）
     * - INC_EQ_DESC_EQ：先非严格递增后非严格递减，找到最大值区间后验证
     * - DESC_EQ_INC_EQ：先非严格递减后非严格递增，找到最小值区间后验证
     * - EQ：所有元素相等
     * 
     * 计算说明：
     * - 使用BigDecimal进行高精度计算，避免浮点数精度问题
     * - step=0.0表示不应用步进，只验证递增/递减关系
     * - 对于复合序列（如INC_DIFF_DESC_DIFF），会先找到转折点，然后分别验证前后两部分
     * 
     * 注意事项：
     * - 复合序列的转折点不能是首尾元素，否则验证失败
     * - 对于允许相等的序列类型，会处理连续相等的情况
     * - 所有计算都使用BigDecimal，确保精度
     * 
     * @param type 序列类型枚举
     * @param step 步进值，0.0表示不应用步进
     * @param values 待验证的数值序列
     * @return true表示序列符合规则，false表示不符合
     */
    private fun validate(type: SeriesTypeEnum, step: Double, vararg values: BigDecimal): Boolean {
        return when (type) {
            SeriesTypeEnum.INC_DIFF -> {
                var preValue: BigDecimal? = null
                for (value in values) {
                    if (preValue != null) {
                        if (step == 0.0) { // 不应用步进
                            if (preValue >= value) {
                                return false
                            }
                        } else {
                            if (preValue + BigDecimal(step) != value) {
                                return false
                            }
                        }
                    }
                    preValue = value
                }
                true
            }
            SeriesTypeEnum.DESC_DIFF -> {
                validate(SeriesTypeEnum.INC_DIFF, step, *values.reversed().toTypedArray())
            }
            SeriesTypeEnum.INC_DIFF_DESC_DIFF -> {
                val maxValueIndex = values.indexOf(values.maxOrNull())
                if (maxValueIndex == values.lastIndex) {
                    return false
                }
                val incDiffValues = values.copyOfRange(0, maxValueIndex + 1)
                val incDiffPass = validate(SeriesTypeEnum.INC_DIFF, step, *incDiffValues)
                if (incDiffPass) {
                    val descDiffValues = values.copyOfRange(maxValueIndex, values.size)
                    validate(SeriesTypeEnum.DESC_DIFF, step, *descDiffValues)
                } else {
                    false
                }
            }
            SeriesTypeEnum.DESC_DIFF_INC_DIFF -> {
                val minValueIndex = values.indexOf(values.minOrNull())
                if (minValueIndex == values.lastIndex) {
                    return false
                }
                val descDiffValues = values.copyOfRange(0, minValueIndex + 1)
                val descDiffPass = validate(SeriesTypeEnum.DESC_DIFF, step, *descDiffValues)
                if (descDiffPass) {
                    val incDiffValues = values.copyOfRange(minValueIndex, values.size)
                    validate(SeriesTypeEnum.INC_DIFF, step, *incDiffValues)
                } else {
                    false
                }
            }
            SeriesTypeEnum.DIFF -> {
                val diff = values.toSet().size == values.size
                if (!diff) {
                    return false
                } else if (step != 0.0) {
                    var preValue: BigDecimal? = null
                    for (value in values) {
                        if (preValue != null) {
                            if ((preValue - value).abs() != BigDecimal(step)) {
                                return false
                            }
                        }
                        preValue = value
                    }
                }
                true
            }
            SeriesTypeEnum.INC_EQ -> {
                var preValue: BigDecimal? = null
                for (value in values) {
                    if (preValue != null) {
                        if (step == 0.0) { // 不应用步进
                            if (preValue > value) {
                                return false
                            }
                        } else {
                            if (preValue != value && preValue + BigDecimal(step) != value) {
                                return false
                            }
                        }
                    }
                    preValue = value
                }
                true
            }
            SeriesTypeEnum.DESC_EQ -> {
                validate(SeriesTypeEnum.INC_EQ, step, *values.reversed().toTypedArray())
            }
            SeriesTypeEnum.INC_EQ_DESC_EQ -> {
                val maxValue = values.maxOrNull()
                val maxValueStartIndex = values.indexOf(maxValue)
                if (maxValueStartIndex == 0 || maxValueStartIndex == values.lastIndex) {
                    return false
                }
                var maxValueEndIndex = maxValueStartIndex
                for (index in maxValueStartIndex until values.lastIndex) {
                    if (values[index] == maxValue) {
                        maxValueEndIndex = index
                    } else {
                        break
                    }
                }
                val incEqValues = values.copyOfRange(0, maxValueStartIndex + 1)
                val incEqPass = validate(SeriesTypeEnum.INC_EQ, step, *incEqValues)
                if (incEqPass) {
                    val descEqValues = values.copyOfRange(maxValueEndIndex, values.size)
                    validate(SeriesTypeEnum.DESC_EQ, step, *descEqValues)
                } else {
                    false
                }
            }
            SeriesTypeEnum.DESC_EQ_INC_EQ -> {
                val minValue = values.minOrNull()
                val minValueStartIndex = values.indexOf(minValue)
                if (minValueStartIndex == 0 || minValueStartIndex == values.lastIndex) {
                    return false
                }
                var minValueEndIndex = minValueStartIndex
                for (index in minValueStartIndex until values.lastIndex) {
                    if (values[index] == minValue) {
                        minValueEndIndex = index
                    } else {
                        break
                    }
                }
                val descEqValues = values.copyOfRange(0, minValueStartIndex + 1)
                val descEqPass = validate(SeriesTypeEnum.DESC_EQ, step, *descEqValues)
                if (descEqPass) {
                    val incEqValues = values.copyOfRange(minValueEndIndex, values.size)
                    validate(SeriesTypeEnum.INC_EQ, step, *incEqValues)
                } else {
                    false
                }
            }
            SeriesTypeEnum.EQ -> {
                values.toSet().size == 1
            }
        }
    }

}
