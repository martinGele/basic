package com.refactoring.excercise

import org.junit.Test
import org.junit.Assert.assertEquals

/**
 * Unit tests for the CounterViewModel.
 *
 * These tests verify that the counter increments correctly and can be reset.
 */
class CounterViewModelTest {

    private val viewModel = CounterViewModel()

    @Test
    fun counterStartsAtZero() {
        assertEquals(0, viewModel.count)
    }

    @Test
    fun incrementIncrementsCounter() {
        viewModel.increment()
        assertEquals(1, viewModel.count)

        viewModel.increment()
        assertEquals(2, viewModel.count)
    }

    @Test
    fun resetResetsCounter() {
        viewModel.increment()
        viewModel.increment()
        viewModel.increment()
        assertEquals(3, viewModel.count)

        viewModel.reset()
        assertEquals(0, viewModel.count)
    }

    @Test
    fun multipleIncrementsWork() {
        repeat(5) { viewModel.increment() }
        assertEquals(5, viewModel.count)
    }
}
