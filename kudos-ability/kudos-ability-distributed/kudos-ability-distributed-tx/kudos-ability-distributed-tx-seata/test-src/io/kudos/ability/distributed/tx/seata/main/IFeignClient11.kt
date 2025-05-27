package io.kudos.ability.distributed.tx.seata.main

import org.springframework.cloud.openfeign.FeignClient

/**
 * 微服务应用1的Feign客户端
 *
 * @author will
 * @since 5.1.1
 */
@FeignClient(value = "ms11")
interface IFeignClient11 : IClient1
