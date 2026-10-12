package com.smoothy.kidguard

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.smoothy.kidguard.util.PreferencesHelper
import com.smoothy.kidguard.util.ThemeHelper

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: PreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = PreferencesHelper(this)
        ThemeHelper.applyTheme(this, prefs)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupThemeSelector()
        setupColorSelector()
        setupFontSelector()
        setupSecuritySettings()
    }

    private fun setupThemeSelector() {
        val rgTheme: RadioGroup = findViewById(R.id.rgThemeMode)
        when (prefs.themeMode) {
            0 -> rgTheme.check(R.id.rbThemeDark)
            1 -> rgTheme.check(R.id.rbThemeAmoled)
            2 -> rgTheme.check(R.id.rbThemeLight)
        }

        rgTheme.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbThemeDark -> prefs.themeMode = 0
                R.id.rbThemeAmoled -> prefs.themeMode = 1
                R.id.rbThemeLight -> prefs.themeMode = 2
            }
            recreate()
        }
    }

    private fun setupColorSelector() {
        findViewById<android.view.View>(R.id.colorIndigo).setOnClickListener {
            prefs.accentColor = 0xFF6366F1.toInt()
            Toast.makeText(this, "Indigo Accent Applied", Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.colorEmerald).setOnClickListener {
            prefs.accentColor = 0xFF10B981.toInt()
            Toast.makeText(this, "Emerald Accent Applied", Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.colorOcean).setOnClickListener {
            prefs.accentColor = 0xFF0EA5E9.toInt()
            Toast.makeText(this, "Ocean Accent Applied", Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.colorPurple).setOnClickListener {
            prefs.accentColor = 0xFFA855F7.toInt()
            Toast.makeText(this, "Purple Accent Applied", Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.colorCoral).setOnClickListener {
            prefs.accentColor = 0xFFF43F5E.toInt()
            Toast.makeText(this, "Coral Accent Applied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFontSelector() {
        val rgFont: RadioGroup = findViewById(R.id.rgFontStyle)
        when (prefs.fontStyle) {
            0 -> rgFont.check(R.id.rbFontModern)
            1 -> rgFont.check(R.id.rbFontRounded)
            2 -> rgFont.check(R.id.rbFontSerif)
        }

        rgFont.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbFontModern -> prefs.fontStyle = 0
                R.id.rbFontRounded -> prefs.fontStyle = 1
                R.id.rbFontSerif -> prefs.fontStyle = 2
            }
            Toast.makeText(this, "Typography preference saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSecuritySettings() {
        val switchBiometrics: SwitchMaterial = findViewById(R.id.switchBiometrics)
        switchBiometrics.isChecked = prefs.isBiometricsEnabled
        switchBiometrics.setOnCheckedChangeListener { _, isChecked ->
            prefs.isBiometricsEnabled = isChecked
        }

        val switchFakeCrash: SwitchMaterial = findViewById(R.id.switchFakeCrash)
        switchFakeCrash.isChecked = prefs.isFakeCrashEnabled
        switchFakeCrash.setOnCheckedChangeListener { _, isChecked ->
            prefs.isFakeCrashEnabled = isChecked
        }

        findViewById<MaterialButton>(R.id.btnChangePin).setOnClickListener {
            showChangePinDialog()
        }
    }

    private fun showChangePinDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.item_app, null)
        val input = EditText(this).apply {
            hint = "Enter new 4-digit PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.change_pin))
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newPin = input.text.toString().trim()
                if (newPin.length == 4) {
                    prefs.masterPin = newPin
                    Toast.makeText(this, getString(R.string.pin_set_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
