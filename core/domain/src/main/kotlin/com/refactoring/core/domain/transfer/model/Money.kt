package com.refactoring.core.domain.transfer.model

import java.math.BigDecimal
import java.math.RoundingMode

data class Money(val amount: BigDecimal, val currency: Currency) {

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch: $currency vs ${other.currency}" }
        return Money(amount + other.amount, currency)
    }

    operator fun compareTo(other: Money): Int {
        require(currency == other.currency) { "Currency mismatch: $currency vs ${other.currency}" }
        return amount.compareTo(other.amount)
    }

    fun isPositive(): Boolean = amount.signum() > 0

    override fun toString(): String = "${amount.setScale(2, RoundingMode.HALF_UP)} $currency"
}
