package com.refactoring.excercise.transfer.service

import android.os.Build
import androidx.annotation.RequiresApi
import com.refactoring.excercise.transfer.model.TransferRequest
import com.refactoring.excercise.transfer.model.TransferResult
import java.time.Clock
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

@RequiresApi(Build.VERSION_CODES.O)
class MoneyTransferService(
    private val feeCalculator: FeeCalculator = FeeCalculator(),
    private val clock: Clock = Clock.systemUTC(),
    private val logger: Logger = Logger { println(it) },
) {
    private val nextId = AtomicInteger(0)

    fun transfer(request: TransferRequest): TransferResult {
        if (!request.amount.isPositive()) {
            return failure(TransferResult.Failure.InvalidAmount, "invalid amount")
        }

        if (request.senderBalance < request.amount) {
            return failure(TransferResult.Failure.InsufficientFunds, "insufficient funds")
        }

        val fee = feeCalculator.calculate(request)
        val total = request.amount + fee

        if (request.senderBalance < total) {
            return failure(
                TransferResult.Failure.InsufficientFunds,
                "insufficient funds (fee was $fee)",
            )
        }

        val id = nextId.incrementAndGet()
        logger.log(
            "[${Instant.now(clock)}] tx#$id OK ${request.fromAccount} -> ${request.toAccount} " +
                "amount=${request.amount} fee=$fee total=$total",
        )
        return TransferResult.Success(id, total)
    }

    private fun failure(result: TransferResult.Failure, reason: String): TransferResult.Failure {
        logger.log("[${Instant.now(clock)}] tx FAIL $reason")
        return result
    }
}
