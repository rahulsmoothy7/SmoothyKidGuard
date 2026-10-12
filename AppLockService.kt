package com.smoothy.kidguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.smoothy.kidguard.LockOverlayActivity
import com.smoothy.kidguard.MainActivity
import com.smoothy.kidguard.R
import com.smoothy.kidguard.util.PreferencesHelper

class AppLockService : Service() {

    private lateinit var prefs: PreferencesHelper
    private lateinit var usageStatsManager: UsageStatsManager
    private val handler = Handler(Looper.getMainLooper())
    private var lastForegroundPackage: String? = null
    private var isRunning = false

    companion object {
        private const val CHANNEL_ID = "kidguard_service_channel"
        private const val NOTIFICATION_ID = 1001
        private const val CHECK_INTERVAL_MS = 400L // Fast and battery optimized
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                // When screen turns off, re-lock all temporarily unlocked apps
                PreferencesHelper.clearTemporaryUnlocks()
                lastForegroundPackage = null
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesHelper(this)
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        createNotificationChannel()
        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            handler.post(monitorRunnable)
        }
        return START_STICKY
    }

    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                checkForegroundApp()
                handler.postDelayed(this, CHECK_INTERVAL_MS)
            }
        }
    }

    private fun checkForegroundApp() {
        if (!prefs.isKidModeActive) return

        val foregroundPackage = getForegroundPackageName() ?: return

        // Skip our own app activities
        if (foregroundPackage == packageName) return

        if (foregroundPackage != lastForegroundPackage) {
            lastForegroundPackage = foregroundPackage

            if (prefs.isAppLocked(foregroundPackage)) {
                launchLockOverlay(foregroundPackage)
            }
        }
    }

    private fun getForegroundPackageName(): String? {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 2000 // 2-second sliding window

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var currentForeground: String? = null

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                currentForeground = event.packageName
            }
        }
        return currentForeground
    }

    private fun launchLockOverlay(targetPackage: String) {
        val lockIntent = Intent(this, LockOverlayActivity::class.java).apply {
            putExtra(LockOverlayActivity.EXTRA_TARGET_PACKAGE, targetPackage)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(lockIntent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Smoothy KidGuard Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors app access to protect kids and parents"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Smoothy KidGuard Active")
            .setContentText("Parental shield is running. Tap to manage.")
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(monitorRunnable)
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
