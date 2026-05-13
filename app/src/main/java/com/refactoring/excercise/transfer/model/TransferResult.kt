package com.refactoring.excercise.transfer.model

sealed interface TransferResult {
    data class Success(val transactionId: Int, val total: Money) : TransferResult

    sealed interface Failure : TransferResult {
        data object InvalidAmount : Failure
        data object InsufficientFunds : Failure
    }
}
