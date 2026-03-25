package io.kudos.base.model.response

/**
 * 错误明细
 *
 * 用于承载一次请求失败时的结构化错误信息。
 * 适用于：
 * 1. 参数校验失败
 * 2. 字段级错误提示
 * 3. 批量操作失败明细
 * 4. 复杂业务校验失败
 *
 * @author K
 * @author AI: ChatGPT
 * @since 1.0.0
 */
data class ErrorDetail(

    /**
     * 明细错误码
     *
     * 用于标识这一条具体错误的类型。
     * 与外层 ApiResponse.code 不同，外层 code 表示本次请求整体失败类型，
     * 这里的 code 表示某一条具体错误的细分类别。
     *
     * 例如：
     * - REQUIRED
     * - INVALID_FORMAT
     * - VALUE_TOO_LARGE
     * - USERNAME_EXISTS
     *
     * 可为空：
     * - 如果当前场景只需要 message，不需要更细粒度的错误分类，则可不填。
     */
    val code: String? = null,

    /**
     * 出错字段名
     *
     * 主要用于字段级校验失败场景，便于前端将错误绑定到具体表单项。
     *
     * 例如：
     * - name
     * - age
     * - address.city
     * - items[0].price
     *
     * 可为空：
     * - 如果不是字段错误，而是某条记录、某个业务对象、某行数据出错，则可不填。
     */
    val field: String? = null,

    /**
     * 出错目标
     *
     * 用于描述当前错误所针对的对象、记录、行、元素或业务目标。
     * 当 field 不足以表达错误位置时，可以用 target 补充说明。
     *
     * 例如：
     * - row[3]
     * - user:1001
     * - order:20260001
     * - items[2]
     *
     * 可为空：
     * - 如果 field 已足够表达错误位置，或者无需标识具体目标，则可不填。
     */
    val target: String? = null,

    /**
     * 错误消息
     *
     * 对当前这一条错误的文字说明。
     * 这是给前端或调用方直接展示的核心文案。
     *
     * 例如：
     * - 名称不能为空
     * - 年龄不能小于0
     * - 第3行手机号格式错误
     * - 订单状态不允许支付
     *
     * 一般不建议为空。
     */
    val message: String,

    /**
     * 被拒绝的值
     *
     * 用于记录导致本次错误的原始输入值，便于排查问题或前端展示。
     *
     * 例如：
     * - ""                // 空字符串
     * - -1                // 非法数值
     * - "abc@@"           // 格式错误的输入
     * - map/list/object   // 某些复杂请求体片段
     *
     * 可为空：
     * - 某些场景下不方便返回，或者出于安全考虑不应回显原值。
     */
    val rejectedValue: Any? = null
)