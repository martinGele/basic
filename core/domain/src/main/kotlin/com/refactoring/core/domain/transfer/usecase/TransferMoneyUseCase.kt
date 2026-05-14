package com.refactoring.core.domain.transfer.usecase

import com.refactoring.core.domain.di.IoDispatcher
import com.refactoring.core.domain.transfer.model.Money
import com.refactoring.core.domain.transfer.model.TransferRequest
import com.refactoring.core.domain.transfer.model.TransferResult
import com.refactoring.core.domain.transfer.repository.AccountRepository
import com.refactoring.core.domain.transfer.service.MoneyTransferService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TransferMoneyUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transferService: MoneyTransferService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(
        fromAccount: String,
        toAccount: String,
        amount: Money,
        senderCountry: String,
        recipientCountry: String,
    ): TransferResult = withContext(ioDispatcher) {
        val balance = accountRepository.getBalance(fromAccount)

        val request = TransferRequest(
            fromAccount = fromAccount,
            toAccount = toAccount,
            amount = amount,
            senderCountry = senderCountry,
            recipientCountry = recipientCountry,
            senderBalance = balance,
        )

        val result = transferService.transfer(request)
        if (result is TransferResult.Success) {
            accountRepository.recordTransfer(result)
        }
        result
    }
}
