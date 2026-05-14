package com.refactoring.core.domain.transfer.service

import com.refactoring.core.domain.transfer.model.Currency
import com.refactoring.core.domain.transfer.model.Money
import com.refactoring.core.domain.transfer.model.TransferRequest
import com.refactoring.core.domain.transfer.model.TransferResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class MoneyTransferServiceTest {

    private val fixedClock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC)
    private val capturedLogs = mutableListOf<String>()
    private val logger = Logger { capturedLogs += it }

    private val service = MoneyTransferService(
        feeCalculator = FeeCalculator(),
        clock = fixedClock,
        logger = logger,
    )

    private fun money(amount: String, currency: Currency = Currency.EUR) = Money(BigDecimal(amount), currency)

    private fun request(
        amount: Money = money("100.00"),
        senderBalance: Money = money("1000.00"),
        senderCountry: String = "DE",
        recipientCountry: String = "DE",
    ) = TransferRequest(
        fromAccount = "DE89-001",
        toAccount = "DE89-002",
        amount = amount,
        senderCountry = senderCountry,
        recipientCountry = recipientCountry,
        senderBalance = senderBalance,
    )

    @Test
    fun `domestic transfer below threshold succeeds with zero fee`() {
        val result = service.transfer(request(amount = money("100.00")))

        assertTrue(result is TransferResult.Success)
        result as TransferResult.Success
        assertEquals(BigDecimal("100.00"), result.total.amount)
        assertEquals(1, result.transactionId)
    }

    @Test
    fun `domestic transfer above 10k applies flat fee`() {
        val result = service.transfer(
            request(amount = money("15000.00"), senderBalance = money("20000.00")),
        )

        assertTrue(result is TransferResult.Success)
        result as TransferResult.Success
        assertEquals(BigDecimal("15001.99"), result.total.amount)
    }

    @Test
    fun `international transfer in low tier uses 1 percent rate plus 0_99 flat`() {
        val result = service.transfer(
            request(
                amount = money("500.00"),
                senderCountry = "DE",
                recipientCountry = "FR",
            ),
        )

        assertTrue(result is TransferResult.Success)
        result as TransferResult.Success
        assertEquals(BigDecimal("505.99"), result.total.amount)
    }

    @Test
    fun `international transfer in mid tier uses 1_5 percent rate plus 1_50 flat`() {
        val result = service.transfer(
            request(
                amount = money("2000.00"),
                senderCountry = "DE",
                recipientCountry = "FR",
                senderBalance = money("3000.00"),
            ),
        )

        assertTrue(result is TransferResult.Success)
        result as TransferResult.Success
        assertEquals(BigDecimal("2031.50"), result.total.amount)
    }

    @Test
    fun `international transfer in high tier uses 2_5 percent rate plus 2_50 flat`() {
        val result = service.transfer(
            request(
                amount = money("8000.00"),
                senderCountry = "DE",
                recipientCountry = "FR",
                senderBalance = money("10000.00"),
            ),
        )

        assertTrue(result is TransferResult.Success)
        result as TransferResult.Success
        assertEquals(BigDecimal("8202.50"), result.total.amount)
    }

    @Test
    fun `GBP transfer applies 15 percent surcharge on fee`() {
        val result = service.transfer(
            request(
                amount = money("500.00", Currency.GBP),
                senderCountry = "DE",
                recipientCountry = "FR",
                senderBalance = money("1000.00", Currency.GBP),
            ),
        )

        assertTrue(result is TransferResult.Success)
        result as TransferResult.Success
        assertEquals(BigDecimal("506.89"), result.total.amount)
    }

    @Test
    fun `zero amount is rejected as InvalidAmount`() {
        val result = service.transfer(request(amount = money("0.00")))

        assertEquals(TransferResult.Failure.InvalidAmount, result)
    }

    @Test
    fun `negative amount is rejected as InvalidAmount`() {
        val result = service.transfer(request(amount = money("-10.00")))

        assertEquals(TransferResult.Failure.InvalidAmount, result)
    }

    @Test
    fun `transfer exceeding balance fails with InsufficientFunds`() {
        val result = service.transfer(
            request(amount = money("100.00"), senderBalance = money("50.00")),
        )

        assertEquals(TransferResult.Failure.InsufficientFunds, result)
    }

    @Test
    fun `transfer fails when balance covers amount but not fee`() {
        val result = service.transfer(
            request(
                amount = money("500.00"),
                senderBalance = money("505.00"),
                senderCountry = "DE",
                recipientCountry = "FR",
            ),
        )

        assertEquals(TransferResult.Failure.InsufficientFunds, result)
    }

    @Test
    fun `transaction ids increment across successful transfers`() {
        val r1 = service.transfer(request()) as TransferResult.Success
        val r2 = service.transfer(request()) as TransferResult.Success
        val r3 = service.transfer(request()) as TransferResult.Success

        assertEquals(1, r1.transactionId)
        assertEquals(2, r2.transactionId)
        assertEquals(3, r3.transactionId)
    }

    @Test
    fun `success log contains transaction id and amounts`() {
        service.transfer(request(amount = money("100.00")))

        assertEquals(1, capturedLogs.size)
        val line = capturedLogs.single()
        assertTrue(line, line.contains("tx#1 OK"))
        assertTrue(line, line.contains("amount=100.00 EUR"))
    }

    @Test
    fun `failure log captures reason`() {
        service.transfer(request(amount = money("0.00")))

        assertEquals(1, capturedLogs.size)
        assertTrue(capturedLogs.single().contains("FAIL invalid amount"))
    }
}
