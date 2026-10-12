package com.smoothy.kidguard

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.smoothy.kidguard.util.PreferencesHelper
import com.smoothy.kidguard.util.ThemeHelper
import java.util.concurrent.Executor

class LockOverlayActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
    }

    private lateinit var prefs: PreferencesHelper
    private var targetPackage: String? = null
    private val enteredPin = StringBuilder()

    private lateinit var dots: Array<View>
    private lateinit var layoutPinContainer: LinearLayout
    private lateinit var layoutFakeCrash: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = PreferencesHelper(this)
        ThemeHelper.applyTheme(this, prefs)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_overlay)

        targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)

        initViews()
        setupAppIdentity()
        setupKeypad()
        setupBiometrics()

        // Intercept back button to return to launcher home
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goToHomeScreen()
            }
        })
    }

    private fun initViews() {
        layoutPinContainer = findViewById(R.id.layoutPinContainer)
        layoutFakeCrash = findViewById(R.id.layoutFakeCrash)

        dots = arrayOf(
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3),
            findViewById(R.id.dot4)
        )

        val btnExitFakeCrash: MaterialButton = findViewById(R.id.btnExitFakeCrash)
        btnExitFakeCrash.setOnClickListener {
            goToHomeScreen()
        }

        if (prefs.isFakeCrashEnabled) {
            layoutPinContainer.visibility = View.GONE
            layoutFakeCrash.visibility = View.VISIBLE
        } else {
            layoutPinContainer.visibility = View.VISIBLE
            layoutFakeCrash.visibility = View.GONE
        }
    }

    private fun setupAppIdentity() {
        val tvTitle: TextView = findViewById(R.id.tvLockedAppTitle)
        val ivIcon: ImageView = findViewById(R.id.ivLockShield)

        targetPackage?.let { pkg ->
            try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                val appLabel = packageManager.getApplicationLabel(appInfo).toString()
                tvTitle.text = "$appLabel is Restricted"
                ivIcon.setImageDrawable(packageManager.getApplicationIcon(appInfo))
            } catch (e: PackageManager.NameNotFoundException) {
                tvTitle.text = getString(R.string.app_is_locked)
            }
        }
    }

    private fun setupKeypad() {
        val digitIds = arrayOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )

        for (id in digitIds) {
            val tv = findViewById<TextView>(id)
            tv.setOnClickListener {
                if (enteredPin.length < 4) {
                    enteredPin.append(tv.text)
                    vibrateFeedback()
                    updateDots()
                    if (enteredPin.length == 4) {
                        validatePin()
                    }
                }
            }
        }

        findViewById<ImageView>(R.id.btnBackspace).setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin.deleteCharAt(enteredPin.length - 1)
                vibrateFeedback()
                updateDots()
            }
        }

        findViewById<ImageView>(R.id.btnBiometric).setOnClickListener {
            triggerBiometricPrompt()
        }
    }

    private fun updateDots() {
        val activeColor = prefs.accentColor
        val inactiveColor = ContextCompat.getColor(this, R.color.card_border_dark)

        for (i in 0 until 4) {
            if (i < enteredPin.length) {
                dots[i].setBackgroundColor(activeColor)
            } else {
                dots[i].setBackgroundColor(inactiveColor)
            }
        }
    }

    private fun validatePin() {
        val masterPin = prefs.masterPin
        if (enteredPin.toString() == masterPin) {
            targetPackage?.let { PreferencesHelper.markAppUnlocked(it) }
            finish()
        } else {
            vibrateError()
            Toast.makeText(this, getString(R.string.wrong_pin), Toast.LENGTH_SHORT).show()
            enteredPin.clear()
            updateDots()
        }
    }

    private fun setupBiometrics() {
        if (prefs.isBiometricsEnabled && !prefs.isFakeCrashEnabled) {
            triggerBiometricPrompt()
        }
    }

    private fun triggerBiometricPrompt() {
        val executor: Executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                targetPackage?.let { PreferencesHelper.markAppUnlocked(it) }
                finish()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // Stay on PIN screen
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setSubtitle(getString(R.string.biometric_prompt_subtitle))
            .setNegativeButtonText("Use PIN")
            .build()

        try {
            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            // Biometrics not available on device
        }
    }

    private fun goToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    private fun vibrateFeedback() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(30)
        }
    }

    private fun vibrateError() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1))
        } else {
            vibrator.vibrate(200)
        }
    }
}
