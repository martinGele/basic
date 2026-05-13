package com.refactoring.excercise.transfer.presentation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.refactoring.excercise.transfer.model.Currency
import com.refactoring.excercise.transfer.model.Money
import com.refactoring.excercise.transfer.model.TransferResult
import com.refactoring.excercise.transfer.repository.AccountRepository
import com.refactoring.excercise.transfer.usecase.TransferMoneyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MVI ViewModel for the transfer screen.
 *
 * Contract:
 *   - [state]   — the single source of truth the View renders (StateFlow).
 *   - [effects] — one-shot events (errors, navigation) the View consumes once (Flow).
 *   - [onIntent] — the *only* way the View talks back. Every user action is a
 *                  [TransferIntent] dispatched through this entry point.
 *
 * Why MVI over MVVM:
 *   - One reducer = one place to read every state transition. No scattered onFoo() methods.
 *   - Intents are values, so they're trivially serializable, loggable, and replayable.
 *   - The View has zero behavior — it renders [state] and emits [TransferIntent]s.
 */
@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class TransferViewModel @Inject constructor(
    private val transferMoney: TransferMoneyUseCase,
    accountRepository: AccountRepository,
) : ViewModel() {

    private val mutableState = MutableStateFlow(TransferState())

    /**
     * Merged state stream: UI-owned fields combined with data-layer balance. See the
     * MVVM version's KDoc on `stateIn` + `WhileSubscribed(5_000)` for the same rationale.
     */
    val state: StateFlow<TransferState> = combine(
        mutableState,
        accountRepository.observeBalance(DEFAULT_ACCOUNT),
    ) { state, balance -> state.copy(balance = balance) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = mutableState.value,
        )

    // Channel for one-shot effects: each emit is delivered to exactly one consumer.
    // Buffered so emits from the ViewModel don't suspend if the View is briefly detached.
    private val effectChannel = Channel<TransferEffect>(Channel.BUFFERED)
    val effects: Flow<TransferEffect> = effectChannel.receiveAsFlow()

    /**
     * The single entry point. Every user action flows through here.
     *
     * Adding a new TransferIntent will force a compiler error here until handled —
     * exhaustiveness over a sealed hierarchy is the whole point of MVI.
     */
    fun onIntent(intent: TransferIntent) {
        when (intent) {
            is TransferIntent.RecipientChanged ->
                mutableState.update { it.copy(toAccount = intent.value, lastResult = null) }

            is TransferIntent.AmountChanged ->
                mutableState.update { it.copy(amountInput = intent.value, lastResult = null) }

            TransferIntent.SubmitClicked -> submit()

            TransferIntent.BannerDismissed ->
                mutableState.update { it.copy(lastResult = null) }
        }
    }

    private fun submit() {
        val current = mutableState.value

        val amount = parseAmount(current.amountInput) ?: run {
            mutableState.update { it.copy(lastResult = TransferState.ResultBanner.InvalidAmount) }
            return
        }

        mutableState.update { it.copy(isSubmitting = true, lastResult = null) }

        viewModelScope.launch {
            val result = transferMoney(
                fromAccount = current.fromAccount,
                toAccount = current.toAccount,
                amount = amount,
                senderCountry = "DE",
                recipientCountry = "DE",
            )
            mutableState.update {
                it.copy(
                    isSubmitting = false,
                    lastResult = result.toBanner(),
                    amountInput = if (result is TransferResult.Success) "" else it.amountInput,
                )
            }
            // Demonstrate the Effect channel: surface a one-shot error toast on failure.
            // (The banner already lives in state; an effect is what you'd use for a
            // transient snackbar or navigation instead.)
            if (result is TransferResult.Failure.InsufficientFunds) {
                effectChannel.send(TransferEffect.ShowError("Not enough funds"))
            }
        }
    }

    private fun parseAmount(input: String): Money? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val parsed = trimmed.toBigDecimalOrNull() ?: return null
        if (parsed.signum() <= 0) return null
        return Money(parsed, Currency.EUR)
    }

    private fun TransferResult.toBanner(): TransferState.ResultBanner = when (this) {
        is TransferResult.Success -> TransferState.ResultBanner.Success(transactionId, total)
        TransferResult.Failure.InvalidAmount -> TransferState.ResultBanner.InvalidAmount
        TransferResult.Failure.InsufficientFunds -> TransferState.ResultBanner.InsufficientFunds
    }

    companion object {
        private const val DEFAULT_ACCOUNT = "DE89-001"
    }
}
