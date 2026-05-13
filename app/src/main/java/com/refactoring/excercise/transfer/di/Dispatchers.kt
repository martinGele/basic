package com.refactoring.excercise.transfer.di

import javax.inject.Qualifier

// Qualifier annotations let Hilt distinguish between bindings of the same type.
// CoroutineDispatcher has multiple implementations (IO / Main / Default / Unconfined),
// so we tag each provider so injection sites can request a specific one.

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
