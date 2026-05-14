package com.refactoring.core.network.transfer.data

import com.refactoring.core.domain.transfer.model.Money
import com.refactoring.core.domain.transfer.model.TransferResult
import com.refactoring.core.domain.transfer.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryAccountRepository(initialBalance: Money) : AccountRepository {

    private val balanceState = MutableStateFlow(initialBalance)
    private val mutex = Mutex()

    override suspend fun getBalance(accountId: String): Money = balanceState.value

    override fun observeBalance(accountId: String): Flow<Money> = balanceState.asStateFlow()

    override suspend fun recordTransfer(result: TransferResult.Success) = mutex.withLock {
        val current = balanceState.value
        balanceState.value = Money(current.amount - result.total.amount, current.currency)
    }
}
