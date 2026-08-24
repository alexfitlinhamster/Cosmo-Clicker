package com.example.myapplication

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Owns the lifecycle of background simulation loops. */
internal class SimulationJobRegistry(private val scope: CoroutineScope) {
    private val jobs = mutableListOf<Job>()

    fun isRunning(): Boolean = jobs.any(Job::isActive)

    fun launch(block: suspend CoroutineScope.() -> Unit) {
        jobs.removeAll { it.isCompleted }
        jobs += scope.launch(block = block)
    }

    fun cancelAll() {
        jobs.forEach(Job::cancel)
        jobs.clear()
    }
}
