package com.refactoring.core.presentation.transfer

import app.cash.turbine.test
import com.refactoring.core.domain.transfer.model.Currency
import com.refactoring.core.domain.transfer.model.Money
import com.refactoring.core.domain.transfer.model.TransferResult
import com.refactoring.core.domain.transfer.repository.AccountRepository
import com.refactoring.core.domain.transfer.service.FeeCalculator
import com.refactoring.core.domain.transfer.service.MoneyTransferService
import com.refactoring.core.domain.transfer.usecase.TransferMoneyUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TransferViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fixedClock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC)

    private class FakeAccountRepository(initialBalance: Money) : AccountRepository {
        private val balanceState = MutableStateFlow(initialBalance)
        private val mutex = Mutex()

        override suspend fun getBalance(accountId: String): Money = balanceState.value

        override fun observeBalance(accountId: String): Flow<Money> = balanceState.asStateFlow()

        override suspend fun recordTransfer(result: TransferResult.Success) = mutex.withLock {
            val current = balanceState.value
            balanceState.value = Money(current.amount - result.total.amount, current.currency)
        }
    }

    private fun viewModel(initialBalance: String = "1000.00"): TransferViewModel {
        val repo = FakeAccountRepository(
            initialBalance = Money(BigDecimal(initialBalance), Currency.EUR),
        )
        val service = MoneyTransferService(
            feeCalculator = FeeCalculator(),
            clock = fixedClock,
            logger = {},
        )
        val useCase = TransferMoneyUseCase(
            accountRepository = repo,
            transferService = service,
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )
        return TransferViewModel(useCase, repo)
    }

    @Test
    fun `initial state has empty inputs and no banner`() = runTest {
        val vm = viewModel()

        vm.state.test {
            val initial = awaitItem()
            assertEquals("", initial.toAccount)
            assertEquals("", initial.amountInput)
            assertNull(initial.lastResult)
            assertEquals(false, initial.isSubmitting)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SubmitClicked with empty amount shows InvalidAmount banner`() = runTest {
        val vm = viewModel()

        vm.state.test {
            skipItems(1)
            vm.onIntent(TransferIntent.RecipientChanged("DE89-002"))
            vm.onIntent(TransferIntent.AmountChanged(""))
            vm.onIntent(TransferIntent.SubmitClicked)

            val state = expectMostRecentItem()
            assertEquals(TransferState.ResultBanner.InvalidAmount, state.lastResult)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `successful SubmitClicked shows Success banner and clears amount input`() = runTest {
        val vm = viewModel(initialBalance = "1000.00")

        vm.state.test {
            skipItems(1)
            vm.onIntent(TransferIntent.RecipientChanged("DE89-002"))
            vm.onIntent(TransferIntent.AmountChanged("100.00"))
            vm.onIntent(TransferIntent.SubmitClicked)

            var state = awaitItem()
            while (state.isSubmitting || state.lastResult == null) {
                state = awaitItem()
            }

            assertTrue(state.lastResult is TransferState.ResultBanner.Success)
            assertEquals("", state.amountInput)
            assertEquals(false, state.isSubmitting)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `InsufficientFunds path keeps the amount input and emits a ShowError effect`() = runTest {
        val vm = viewModel(initialBalance = "10.00")

        vm.effects.test {
            vm.state.test {
                skipItems(1)
                vm.onIntent(TransferIntent.RecipientChanged("DE89-002"))
                vm.onIntent(TransferIntent.AmountChanged("100.00"))
                vm.onIntent(TransferIntent.SubmitClicked)

                var state = awaitItem()
                while (state.isSubmitting || state.lastResult == null) {
                    state = awaitItem()
                }
                assertEquals(TransferState.ResultBanner.InsufficientFunds, state.lastResult)
                assertEquals("100.00", state.amountInput)
                cancelAndIgnoreRemainingEvents()
            }

            val effect = awaitItem()
            assertTrue(effect is TransferEffect.ShowError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `BannerDismissed clears the banner`() = runTest {
        val vm = viewModel()

        vm.state.test {
            skipItems(1)
            vm.onIntent(TransferIntent.SubmitClicked)
            assertEquals(
                TransferState.ResultBanner.InvalidAmount,
                expectMostRecentItem().lastResult,
            )

            vm.onIntent(TransferIntent.BannerDismissed)
            assertNull(expectMostRecentItem().lastResult)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
