package com.refactoring.excercise.transfer.usecase

import app.cash.turbine.test
import com.refactoring.excercise.transfer.model.Currency
import com.refactoring.excercise.transfer.model.Money
import com.refactoring.excercise.transfer.model.TransferResult
import com.refactoring.excercise.transfer.repository.AccountRepository
import com.refactoring.excercise.transfer.service.FeeCalculator
import com.refactoring.excercise.transfer.service.Logger
import com.refactoring.excercise.transfer.service.MoneyTransferService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TransferMoneyUseCaseTest {

    private val fixedClock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC)

    private val service = MoneyTransferService(
        feeCalculator = FeeCalculator(),
        clock = fixedClock,
        logger = Logger {},
    )

    private fun money(amount: String, currency: Currency = Currency.EUR) = Money(BigDecimal(amount), currency)

    private class FakeAccountRepository(initialBalance: Money) : AccountRepository {
        private val balanceState = MutableStateFlow(initialBalance)
        val recordedTransfers = mutableListOf<TransferResult.Success>()

        override suspend fun getBalance(accountId: String): Money = balanceState.value

        override fun observeBalance(accountId: String) = balanceState.asStateFlow()

        override suspend fun recordTransfer(result: TransferResult.Success) {
            recordedTransfers += result
            balanceState.value = Money(
                balanceState.value.amount - result.total.amount,
                balanceState.value.currency,
            )
        }
    }

    @Test
    fun `useCase returns Success and records the transfer`() = runTest {
        val repo = FakeAccountRepository(initialBalance = money("1000.00"))
        val useCase = TransferMoneyUseCase(
            accountRepository = repo,
            transferService = service,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        val result = useCase(
            fromAccount = "DE89-001",
            toAccount = "DE89-002",
            amount = money("100.00"),
            senderCountry = "DE",
            recipientCountry = "DE",
        )

        assertTrue(result is TransferResult.Success)
        assertEquals(1, repo.recordedTransfers.size)
        assertEquals(BigDecimal("100.00"), repo.recordedTransfers.single().total.amount)
    }

    @Test
    fun `useCase does NOT record when service rejects the transfer`() = runTest {
        val repo = FakeAccountRepository(initialBalance = money("50.00"))
        val useCase = TransferMoneyUseCase(
            accountRepository = repo,
            transferService = service,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        val result = useCase(
            fromAccount = "DE89-001",
            toAccount = "DE89-002",
            amount = money("100.00"),
            senderCountry = "DE",
            recipientCountry = "DE",
        )

        assertEquals(TransferResult.Failure.InsufficientFunds, result)
        assertTrue(repo.recordedTransfers.isEmpty())
    }

    @Test
    fun `observeBalance emits new balance after a successful transfer`() = runTest {
        val repo = FakeAccountRepository(initialBalance = money("1000.00"))
        val useCase = TransferMoneyUseCase(
            accountRepository = repo,
            transferService = service,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        repo.observeBalance("DE89-001").test {
            assertEquals(money("1000.00"), awaitItem())

            useCase(
                fromAccount = "DE89-001",
                toAccount = "DE89-002",
                amount = money("100.00"),
                senderCountry = "DE",
                recipientCountry = "DE",
            )

            assertEquals(money("900.00"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
