package com.android.stepsync.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.android.stepsync.repository.StepRepository

class StepCounterViewModel : ViewModel() {
    val latestTotalSteps: LiveData<Float> = StepRepository.steps
}