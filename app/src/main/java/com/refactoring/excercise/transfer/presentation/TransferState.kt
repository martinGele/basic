package com.refactoring.excercise.transfer.presentation

import com.refactoring.excercise.transfer.model.Money

/**
 * MVI **State** — a single immutable snapshot the View renders.
 *
 * Every visible piece of the screen lives here. The View never holds its own state;
 * it produces [TransferIntent]s and re-renders whatever the new state says.
 */
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
