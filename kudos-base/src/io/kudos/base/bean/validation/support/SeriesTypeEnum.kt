package io.kudos.base.bean.validation.support

/**
 * Series type.
 *
 * @author K
 * @since 1.0.0
 */
enum class SeriesTypeEnum {
    /**
     * Strictly increasing and all distinct.
     */
    INC_DIFF,

    /**
     * Strictly decreasing and all distinct.
     */
    DESC_DIFF,

    /**
     * Increasing then decreasing, all distinct.
     */
    INC_DIFF_DESC_DIFF,

    /**
     * Decreasing then increasing, all distinct.
     */
    DESC_DIFF_INC_DIFF,

    /**
     * All distinct.
     */
    DIFF,

    /**
     * Non-strictly increasing (allows equal).
     */
    INC_EQ,

    /**
     * Non-strictly decreasing (allows equal).
     */
    DESC_EQ,

    /**
     * Non-strictly increasing then non-strictly decreasing.
     */
    INC_EQ_DESC_EQ,

    /**
     * Non-strictly decreasing then non-strictly increasing.
     */
    DESC_EQ_INC_EQ,

    /**
     * All equal.
     */
    EQ
}
