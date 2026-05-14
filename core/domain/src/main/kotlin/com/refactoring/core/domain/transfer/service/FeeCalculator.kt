package com.refactoring.core.domain.transfer.service

import com.refactoring.core.domain.transfer.model.Currency
import com.refactoring.core.domain.transfer.model.Money
import com.refactoring.core.domain.transfer.model.TransferRequest
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class FeeCalculator @Inject constructor() {

    fun calculate(request: TransferRequest): Money {
        val base = if (request.senderCountry != request.recipientCountry) {
            internationalFee(request.amount)
        } else {
            domesticFee(request.amount)
        }

        val adjusted = if (request.amount.currency == Currency.GBP) {
            base.amount * GBP_SURCHARGE
        } else {
            base.amount
        }

        return Money(
            adjusted.setScale(FEE_SCALE, RoundingMode.HALF_UP),
            request.amount.currency,
        )
    }

    private fun internationalFee(amount: Money): Money {
        val (rate, flat) = when {
            amount.amount > HIGH_TIER -> RATE_HIGH to FLAT_HIGH
            amount.amount > MID_TIER -> RATE_MID to FLAT_MID
            else -> RATE_LOW to FLAT_LOW
        }
        return Money(amount.amount * rate + flat, amount.currency)
    }

    private fun domesticFee(amount: Money): Money {
        val fee = if (amount.amount > DOMESTIC_FLAT_THRESHOLD) {
            DOMESTIC_FLAT_FEE
        } else {
            BigDecimal.ZERO
        }
        return Money(fee, amount.currency)
    }

    companion object {
        private const val FEE_SCALE = 2

        private val HIGH_TIER = BigDecimal("5000.00")
        private val MID_TIER = BigDecimal("1000.00")

        private val RATE_HIGH = BigDecimal("0.025")
        private val RATE_MID = BigDecimal("0.015")
        private val RATE_LOW = BigDecimal("0.010")

        private val FLAT_HIGH = BigDecimal("2.50")
        private val FLAT_MID = BigDecimal("1.50")
        private val FLAT_LOW = BigDecimal("0.99")

        private val DOMESTIC_FLAT_THRESHOLD = BigDecimal("10000.00")
        private val DOMESTIC_FLAT_FEE = BigDecimal("1.99")

        private val GBP_SURCHARGE = BigDecimal("1.15")
    }
}
