package com.refactoring.core.domain.transfer.repository

import com.refactoring.core.domain.transfer.model.Money
import com.refactoring.core.domain.transfer.model.TransferResult
import kotlinx.coroutines.flow.Flow

interface AccountRepository {

    suspend fun getBalance(accountId: String): Money

    fun observeBalance(accountId: String): Flow<Money>

    suspend fun recordTransfer(result: TransferResult.Success)
}
