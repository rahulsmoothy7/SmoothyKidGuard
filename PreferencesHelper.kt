package com.smoothy.kidguard.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("smoothy_kidguard_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MASTER_PIN = "key_master_pin"
        private const val KEY_LOCKED_PACKAGES = "key_locked_packages"
        private const val KEY_KID_MODE_ACTIVE = "key_kid_mode_active"
        private const val KEY_THEME_MODE = "key_theme_mode" // 0: Dark, 1: AMOLED, 2: Light
        private const val KEY_ACCENT_COLOR = "key_accent_color" // hex color
        private const val KEY_FONT_STYLE = "key_font_style" // 0: Modern, 1: Rounded, 2: Serif
        private const val KEY_BIOMETRICS_ENABLED = "key_biometrics_enabled"
        private const val KEY_FAKE_CRASH_ENABLED = "key_fake_crash_enabled"
        private const val KEY_PREVENT_UNINSTALL = "key_prevent_uninstall"

        // Ephemeral in-memory unlocked package cache (prevents re-lock loop when parent is using the app)
        private val temporarilyUnlockedApps = mutableSetOf<String>()

        fun markAppUnlocked(packageName: String) {
            temporarilyUnlockedApps.add(packageName)
        }

        fun isAppTemporarilyUnlocked(packageName: String): Boolean {
            return temporarilyUnlockedApps.contains(packageName)
        }

        fun clearTemporaryUnlocks() {
            temporarilyUnlockedApps.clear()
        }
    }

    var masterPin: String?
        get() = prefs.getString(KEY_MASTER_PIN, "1234") // Default 1234
        set(value) = prefs.edit().putString(KEY_MASTER_PIN, value).apply()

    var isPinSet: Boolean
        get() = prefs.contains(KEY_MASTER_PIN)
        set(value) {
            if (!value) prefs.edit().remove(KEY_MASTER_PIN).apply()
        }

    var isKidModeActive: Boolean
        get() = prefs.getBoolean(KEY_KID_MODE_ACTIVE, true) // Active by default
        set(value) = prefs.edit().putBoolean(KEY_KID_MODE_ACTIVE, value).apply()

    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, 0) // Default 0 (Dark)
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value).apply()

    var accentColor: Int
        get() = prefs.getInt(KEY_ACCENT_COLOR, 0xFF6366F1.toInt()) // Default Indigo
        set(value) = prefs.edit().putInt(KEY_ACCENT_COLOR, value).apply()

    var fontStyle: Int
        get() = prefs.getInt(KEY_FONT_STYLE, 0) // 0: Modern Sans
        set(value) = prefs.edit().putInt(KEY_FONT_STYLE, value).apply()

    var isBiometricsEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRICS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRICS_ENABLED, value).apply()

    var isFakeCrashEnabled: Boolean
        get() = prefs.getBoolean(KEY_FAKE_CRASH_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_FAKE_CRASH_ENABLED, value).apply()

    var isPreventUninstallEnabled: Boolean
        get() = prefs.getBoolean(KEY_PREVENT_UNINSTALL, true)
        set(value) = prefs.edit().putBoolean(KEY_PREVENT_UNINSTALL, value).apply()

    fun getLockedPackages(): MutableSet<String> {
        val set = prefs.getStringSet(KEY_LOCKED_PACKAGES, null) ?: mutableSetOf(
            // High-risk apps locked by default for kids safety
            "com.facebook.orca",        // Messenger
            "com.whatsapp",             // WhatsApp
            "com.google.android.youtube",// YouTube
            "com.google.android.dialer",// Phone
            "com.android.settings",     // Settings
            "com.android.vending"       // Play Store
        )
        return HashSet(set)
    }

    fun setAppLocked(packageName: String, isLocked: Boolean) {
        val currentSet = getLockedPackages()
        if (isLocked) {
            currentSet.add(packageName)
        } else {
            currentSet.remove(packageName)
            temporarilyUnlockedApps.remove(packageName)
        }
        prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, currentSet).apply()
    }

    fun isAppLocked(packageName: String): Boolean {
        if (!isKidModeActive) return false
        if (isAppTemporarilyUnlocked(packageName)) return false
        return getLockedPackages().contains(packageName)
    }
}
