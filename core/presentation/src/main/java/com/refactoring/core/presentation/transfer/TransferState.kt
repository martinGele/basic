package com.refactoring.core.presentation.transfer

import com.refactoring.core.domain.transfer.model.Money

data class TransferState(
    val fromAccount: String = "DE89-001",
    val toAccount: String = "",
    val amountInput: String = "",
    val balance: Money? = null,
    val isSubmitting: Boolean = false,
    val lastResult: ResultBanner? = null,
) {
    sealed interface ResultBanner {
        data class Success(val transactionId: Int, val total: Money) : ResultBanner
        data object InvalidAmount : ResultBanner
        data object InsufficientFunds : ResultBanner
    }
}
