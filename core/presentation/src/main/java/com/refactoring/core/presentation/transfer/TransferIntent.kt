package com.refactoring.core.presentation.transfer

sealed interface TransferIntent {
    data class RecipientChanged(val value: String) : TransferIntent
    data class AmountChanged(val value: String) : TransferIntent
    data object SubmitClicked : TransferIntent
    data object BannerDismissed : TransferIntent
}
