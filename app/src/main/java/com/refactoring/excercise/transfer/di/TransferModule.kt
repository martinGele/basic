package com.refactoring.excercise.transfer.di

import android.os.Build
import androidx.annotation.RequiresApi
import com.refactoring.excercise.transfer.data.InMemoryAccountRepository
import com.refactoring.excercise.transfer.model.Currency
import com.refactoring.excercise.transfer.model.Money
import com.refactoring.excercise.transfer.repository.AccountRepository
import com.refactoring.excercise.transfer.service.FeeCalculator
import com.refactoring.excercise.transfer.service.Logger
import com.refactoring.excercise.transfer.service.MoneyTransferService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.math.BigDecimal
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TransferModule {

    @Provides
    @Singleton
    fun provideAccountRepository(): AccountRepository = InMemoryAccountRepository(Money(BigDecimal("1000.00"), Currency.EUR))

    @Provides
    @Singleton
    fun provideFeeCalculator(): FeeCalculator = FeeCalculator()

    @Provides
    @Singleton
    fun provideLogger(): Logger = Logger { println(it) }

    @Provides
    @Singleton
    @RequiresApi(Build.VERSION_CODES.O)
    fun provideMoneyTransferService(
        feeCalculator: FeeCalculator,
        logger: Logger,
    ): MoneyTransferService = MoneyTransferService(
        feeCalculator = feeCalculator,
        logger = logger,
    )

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
