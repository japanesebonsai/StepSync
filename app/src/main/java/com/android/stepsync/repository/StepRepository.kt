package com.android.stepsync.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object StepRepository {
    private val _steps = MutableLiveData<Float>()
    val steps: LiveData<Float> = _steps

    fun updateSteps(newSteps: Float) {
        _steps.postValue(newSteps) // Thread-safe update
    }
}