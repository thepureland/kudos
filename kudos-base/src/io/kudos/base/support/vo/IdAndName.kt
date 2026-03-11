package io.kudos.base.support.vo

/**
 * id和name的封装类
 *
 * @author K
 * @since 1.0.0
 */
data class IdAndName<T> (

    /** 惟一标识 */
    val id: T,

    /** 名称 */
    val name: String

)