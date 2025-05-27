package io.kudos.ability.distributed.tx.seata.main

import org.soul.ability.distributed.tx.seata.data.TestTable

/**
 * 主应用的service接口
 *
 * @author will
 * @since 5.1.1
 */
interface IService {
    fun getById(id: Int?): TestTable?

    val globalTxId: String?

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
