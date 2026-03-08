package io.kudos.base.support

/**
 * id可变的实体接口
 *
 * @param T 实体类型
 * @author K
 * @since 1.0.0
 */
interface IMutableIdEntity<T> : IIdEntity<T> {

    /** 惟一标识 */
    override var id: T

}