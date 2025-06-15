package io.kudos.ability.distributed.tx.seata.main

import org.springframework.cloud.openfeign.FeignClient

/**
 * 微服务应用1的Feign客户端
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient("ms12")
interface IFeignClient12 : IClient1
