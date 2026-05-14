package com.refactoring.excercise.di

import com.refactoring.core.domain.di.DefaultDispatcher
import com.refactoring.core.domain.di.IoDispatcher
import com.refactoring.core.domain.di.MainDispatcher
import com.refactoring.core.domain.transfer.service.FeeCalculator
import com.refactoring.core.domain.transfer.service.Logger
import com.refactoring.core.domain.transfer.service.MoneyTransferService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLogger(): Logger = Logger { println(it) }

    @Provides
    @Singleton
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
