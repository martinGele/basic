package com.refactoring.excercise.transfer.presentation

/**
 * MVI **Intent** — every action the user (or system) can take, modeled as a closed type.
 *
 * Why sealed: the ViewModel's `when (intent)` is exhaustive — adding a new intent forces
 * the compiler to flag every reducer that doesn't handle it. No silent dropped actions.
 */
sealed interface TransferIntent {
    data class RecipientChanged(val value: String) : TransferIntent
    data class AmountChanged(val value: String) : TransferIntent
    data object SubmitClicked : TransferIntent
    data object BannerDismissed : TransferIntent
}
