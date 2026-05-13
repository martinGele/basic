package com.refactoring.excercise.transfer.usecase

import android.os.Build
import androidx.annotation.RequiresApi
import com.refactoring.excercise.transfer.di.IoDispatcher
import com.refactoring.excercise.transfer.model.Money
import com.refactoring.excercise.transfer.model.TransferRequest
import com.refactoring.excercise.transfer.model.TransferResult
import com.refactoring.excercise.transfer.repository.AccountRepository
import com.refactoring.excercise.transfer.service.MoneyTransferService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
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
