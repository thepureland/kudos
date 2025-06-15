package io.kudos.ability.distributed.tx.seata.main

import org.springframework.cloud.openfeign.FeignClient

/**
 * 微服务应用2的Feign客户端
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient("ms22")
interface IFeignClient22 : IClient2
