package com.example.formcheck
import android.content.Context

object LastExerciseStore {
    private const val PREFS_NAME = "formcheck_prefs"
    private const val KEY_LAST_EXERCISE_ID = "last_exercise_id"

    fun save(context: Context, exerciseId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_EXERCISE_ID, exerciseId)
            .apply()
    }

    fun get(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_EXERCISE_ID, null)
    }
}