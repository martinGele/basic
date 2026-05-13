package com.refactoring.excercise.transfer.presentation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TransferScreen(
    modifier: Modifier = Modifier,
    viewModel: TransferViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Consume one-shot effects exactly once. LaunchedEffect re-runs only when the key
    // changes; tying it to `viewModel` keeps the same collector across recompositions.
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TransferEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    TransferScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
internal fun TransferScreenContent(
    state: TransferState,
    onIntent: (TransferIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    // derivedStateOf: caches the boolean and only notifies readers (the Button below)
    // when the *result* changes. Without this, every keystroke into the IBAN/amount
    // fields would mark the Button for recomposition even when the enabled flag stays
    // the same. The lambda re-runs on each state change, but downstream recomposition
    // is gated by structural equality of the derived value.
    val canSubmit by remember {
        derivedStateOf {
            !state.isSubmitting &&
                state.toAccount.isNotBlank() &&
                state.amountInput.toBigDecimalOrNull()?.let { it.signum() > 0 } == true
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Send money", style = MaterialTheme.typography.headlineMedium)

        Text(
            text = "From: ${state.fromAccount}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Balance: ${state.balance?.toString() ?: "—"}",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = state.toAccount,
            onValueChange = { onIntent(TransferIntent.RecipientChanged(it)) },
            label = { Text("Recipient IBAN") },
            singleLine = true,
            enabled = !state.isSubmitting,
        )

        OutlinedTextField(
            value = state.amountInput,
            onValueChange = { onIntent(TransferIntent.AmountChanged(it)) },
            label = { Text("Amount (EUR)") },
            singleLine = true,
            enabled = !state.isSubmitting,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
            ),
        )

        Button(
            onClick = { onIntent(TransferIntent.SubmitClicked) },
            enabled = canSubmit,
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator()
            } else {
                Text("Send")
            }
        }

        state.lastResult?.let { ResultBanner(it) }

        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
private fun ResultBanner(result: TransferState.ResultBanner) {
    val text = when (result) {
        is TransferState.ResultBanner.Success ->
            "Sent! tx#${result.transactionId} • total ${result.total}"
        TransferState.ResultBanner.InvalidAmount -> "Enter a valid amount"
        TransferState.ResultBanner.InsufficientFunds -> "Insufficient funds"
    }
    Text(text, style = MaterialTheme.typography.bodyLarge)
}
