package com.smoothy.kidguard.util

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import com.smoothy.kidguard.R

object ThemeHelper {

    fun applyTheme(activity: Activity, prefs: PreferencesHelper) {
        when (prefs.themeMode) {
            0 -> { // Dark
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            1 -> { // AMOLED
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                activity.setTheme(R.style.Theme_SmoothyKidGuard_Amoled)
            }
            2 -> { // Light
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            else -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
}
