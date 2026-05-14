package com.refactoring.core.presentation.transfer

sealed interface TransferEffect {
    data class ShowError(val message: String) : TransferEffect
}
