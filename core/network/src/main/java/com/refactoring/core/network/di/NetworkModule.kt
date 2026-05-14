package com.refactoring.core.network.di

import com.refactoring.core.domain.transfer.model.Currency
import com.refactoring.core.domain.transfer.model.Money
import com.refactoring.core.domain.transfer.repository.AccountRepository
import com.refactoring.core.network.transfer.data.InMemoryAccountRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.math.BigDecimal
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAccountRepository(): AccountRepository = InMemoryAccountRepository(Money(BigDecimal("1000.00"), Currency.EUR))
}
