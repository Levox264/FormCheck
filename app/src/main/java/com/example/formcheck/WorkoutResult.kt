package com.example.formcheck

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RepJudgement(
    val repNumber: Int,
    val good: Boolean,
    val issues: List<String>,
) : Parcelable

@Parcelize
data class WorkoutResult(
    val exerciseId: String,
    val goodReps: Int,
    val badReps: Int,
    val judgements: List<RepJudgement>,
) : Parcelable