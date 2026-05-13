package com.refactoring.excercise.transfer.model

data class TransferRequest(
    val fromAccount: String,
    val toAccount: String,
    val amount: Money,
    val senderCountry: String,
    val recipientCountry: String,
    val senderBalance: Money,
)
