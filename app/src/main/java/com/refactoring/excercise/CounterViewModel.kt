package com.refactoring.excercise

import androidx.lifecycle.ViewModel

class CounterViewModel : ViewModel() {
    var count = 0
        private set

    fun increment() {
        count++
    }

    fun reset() {
        count = 0
    }
}
