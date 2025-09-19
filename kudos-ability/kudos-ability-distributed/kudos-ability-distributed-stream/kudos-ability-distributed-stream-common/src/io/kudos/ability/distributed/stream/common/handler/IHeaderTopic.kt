package io.kudos.ability.distributed.stream.common.handler

/**
 * @Description stream异常topic接口
 * @Author paul
 * @Date 2022/10/19 15:42
 */
interface IHeaderTopic {
    /**
     * 获取header里topic名称
     */
    val topicName: String?
}
