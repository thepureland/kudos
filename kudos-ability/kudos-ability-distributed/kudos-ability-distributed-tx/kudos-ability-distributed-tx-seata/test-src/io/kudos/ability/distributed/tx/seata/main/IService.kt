package io.kudos.ability.distributed.tx.seata.main

import io.kudos.ability.distributed.tx.seata.data.TestTable


/**
 * 主应用的service接口
 *
 * @author K
 * @since 1.0.0
 */
interface IService {

    fun getById(id: Int): TestTable

    fun getGlobalTxId(): String?

    /**
     * 本地依赖时，正常执行
     */
    fun normalLocal()

    /**
     * 本地依赖时，分支事务异常
     */
    fun onBranchErrorLocal()

    /**
     * 本地依赖时，全局事务异常
     */
    fun onGlobalErrorLocal()

    /**
     * 远程依赖时，正常执行
     */
    fun normalRemote()

    /**
     * 远程依赖时，分支事务异常
     */
    fun onBranchErrorRemote()

    /**
     * 本地依赖时，全局事务异常
     */
    fun onGlobalErrorRemote()

}
