package io.kudos.ability.distributed.tx.seata.main

import io.kudos.ability.distributed.tx.seata.data.TestTable


/**
 * Service interface of the main application.
 *
 * @author K
 * @since 1.0.0
 */
interface IService {

    fun getById(id: Int): TestTable

    fun getGlobalTxId(): String?

    /**
     * Happy path with local dependencies.
     */
    fun normalLocal()

    /**
     * Branch-transaction failure with local dependencies.
     */
    fun onBranchErrorLocal()

    /**
     * Global-transaction failure with local dependencies.
     */
    fun onGlobalErrorLocal()

    /**
     * Happy path with remote dependencies.
     */
    fun normalRemote()

    /**
     * Branch-transaction failure with remote dependencies.
     */
    fun onBranchErrorRemote()

    /**
     * Global-transaction failure with remote dependencies.
     */
    fun onGlobalErrorRemote()

}
