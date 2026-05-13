package com.refactoring.excercise.transfer.presentation

/**
 * MVI **Effect** — one-shot side effects that are NOT part of renderable state.
 *
 * Things that should fire exactly once and never replay on rotation: snackbars,
 * navigation, haptics, launching a chooser. State is for "what the screen looks like
 * right now"; Effect is for "do this thing once then forget".
 *
 * Delivered via a [kotlinx.coroutines.channels.Channel] (not a StateFlow) so each effect
 * is consumed exactly once.
 */
sealed interface TransferEffect {
    data class ShowError(val message: String) : TransferEffect
}
