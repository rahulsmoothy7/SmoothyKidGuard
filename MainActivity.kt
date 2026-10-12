package com.smoothy.kidguard

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.smoothy.kidguard.adapter.AppListAdapter
import com.smoothy.kidguard.model.AppItem
import com.smoothy.kidguard.service.AppLockService
import com.smoothy.kidguard.util.PreferencesHelper
import com.smoothy.kidguard.util.ThemeHelper
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PreferencesHelper
    private lateinit var adapter: AppListAdapter

    private lateinit var cardPermissionWarning: MaterialCardView
    private lateinit var tvPermissionDesc: TextView
    private lateinit var btnGrantPermission: MaterialButton
    private lateinit var switchKidMode: SwitchMaterial
    private lateinit var tvKidModeStatus: TextView
    private lateinit var tvProtectedCount: TextView
    private lateinit var ivShieldIcon: ImageView
    private lateinit var rvApps: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var etSearch: EditText

    private var installedAppList = mutableListOf<AppItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = PreferencesHelper(this)
        ThemeHelper.applyTheme(this, prefs)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        loadInstalledApps()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        startLockServiceIfAllowed()
    }

    private fun initViews() {
        cardPermissionWarning = findViewById(R.id.cardPermissionWarning)
        tvPermissionDesc = findViewById(R.id.tvPermissionDesc)
        btnGrantPermission = findViewById(R.id.btnGrantPermission)
        switchKidMode = findViewById(R.id.switchKidMode)
        tvKidModeStatus = findViewById(R.id.tvKidModeStatus)
        tvProtectedCount = findViewById(R.id.tvProtectedCount)
        ivShieldIcon = findViewById(R.id.ivShieldIcon)
        rvApps = findViewById(R.id.rvAppsList)
        progressBar = findViewById(R.id.progressBar)
        etSearch = findViewById(R.id.etSearchApp)

        rvApps.layoutManager = LinearLayoutManager(this)
        adapter = AppListAdapter(emptyList()) { appItem, isLocked ->
            prefs.setAppLocked(appItem.packageName, isLocked)
            updateProtectedCount()
        }
        rvApps.adapter = adapter

        // Setup Kid Mode initial UI
        switchKidMode.isChecked = prefs.isKidModeActive
        updateKidModeUi(prefs.isKidModeActive)
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        switchKidMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.isKidModeActive = isChecked
            updateKidModeUi(isChecked)
            startLockServiceIfAllowed()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnGrantPermission.setOnClickListener {
            if (!hasUsageAccessPermission()) {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivity(intent)
            } else if (!hasOverlayPermission()) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
    }

    private fun updateKidModeUi(isActive: Boolean) {
        if (isActive) {
            tvKidModeStatus.text = getString(R.string.kid_mode_subtitle_active)
            tvKidModeStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_emerald))
            ivShieldIcon.setColorFilter(ContextCompat.getColor(this, R.color.accent_emerald))
        } else {
            tvKidModeStatus.text = getString(R.string.kid_mode_subtitle_inactive)
            tvKidModeStatus.setTextColor(ContextCompat.getColor(this, R.color.kid_mode_inactive))
            ivShieldIcon.setColorFilter(ContextCompat.getColor(this, R.color.kid_mode_inactive))
        }
    }

    private fun updateProtectedCount() {
        val count = prefs.getLockedPackages().size
        tvProtectedCount.text = getString(R.string.locked_apps_count, count)
    }

    private fun checkPermissions() {
        if (!hasUsageAccessPermission()) {
            cardPermissionWarning.visibility = View.VISIBLE
            tvPermissionDesc.text = getString(R.string.permission_usage_desc)
        } else if (!hasOverlayPermission()) {
            cardPermissionWarning.visibility = View.VISIBLE
            tvPermissionDesc.text = getString(R.string.permission_overlay_desc)
        } else {
            cardPermissionWarning.visibility = View.GONE
        }
    }

    private fun hasUsageAccessPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasOverlayPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
    }

    private fun startLockServiceIfAllowed() {
        if (hasUsageAccessPermission() && hasOverlayPermission()) {
            val serviceIntent = Intent(this, AppLockService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    private fun loadInstalledApps() {
        progressBar.visibility = View.VISIBLE
        thread {
            val pm = packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val lockedSet = prefs.getLockedPackages()
            val list = mutableListOf<AppItem>()

            for (appInfo in packages) {
                // Filter out system apps without launch intent, but keep Settings and Play Store
                val isLaunchable = pm.getLaunchIntentForPackage(appInfo.packageName) != null
                val isCriticalApp = appInfo.packageName == "com.android.settings" || appInfo.packageName == "com.android.vending"

                if (isLaunchable || isCriticalApp) {
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val icon = pm.getApplicationIcon(appInfo)
                    val isLocked = lockedSet.contains(appInfo.packageName)

                    list.add(AppItem(appName, appInfo.packageName, icon, isLocked))
                }
            }

            // Sort: Locked apps first, then alphabetically
            list.sortWith(compareByDescending<AppItem> { it.isLocked }.thenBy { it.appName })

            runOnUiThread {
                installedAppList = list
                adapter.updateData(list)
                progressBar.visibility = View.GONE
                updateProtectedCount()
            }
        }
    }
}
