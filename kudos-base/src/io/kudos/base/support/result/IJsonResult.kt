package io.kudos.base.support.result

import java.io.Serializable

/**
 * 要以json返回的结果对象接口，会自动去除值为null的属性
 *
 * @author K
 * @since 1.0.0
 */
//@JsonInclude(JsonInclude.Include.NON_NULL) //TODO
interface IJsonResult: Serializable