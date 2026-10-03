package com.volumekeys.tap

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.volumekeys.tap.databinding.ActivityKeepAliveBinding

/**
 * Background keep-alive settings page.
 *
 * Three steps:
 *  1. Auto-start permission — jump to the OEM-specific autostart manager.
 *  2. Ignore battery optimizations — request via system intent.
 *  3. Lock in recent tasks — open the recent-apps screen so the user can
 *     manually lock the app (no API exists to do this programmatically).
 */
class KeepAliveActivity : AppCompatActivity() {

    private lateinit var b: ActivityKeepAliveBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityKeepAliveBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        updateBatteryStatus()

        b.btnRequestAutostart.setOnClickListener { requestAutostart() }
        b.btnRequestBattery.setOnClickListener { requestBatteryOptimization() }
        b.btnGotoRecent.setOnClickListener { openRecentTasks() }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        updateBatteryStatus()
    }

    // ---- Battery optimization ----
    private fun updateBatteryStatus() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val ignoring = pm.isIgnoringBatteryOptimizations(packageName)
        if (ignoring) {
            b.batteryStatus.text = getString(R.string.status_granted)
            b.batteryStatus.setTextColor(ContextCompat.getColor(this, R.color.tertiary))
        } else {
            b.batteryStatus.text = getString(R.string.status_not_granted)
            b.batteryStatus.setTextColor(ContextCompat.getColor(this, R.color.error))
        }
    }

    private fun requestBatteryOptimization() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            Toast.makeText(this, R.string.toast_battery_granted, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.toast_need_manual, Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    // ---- Auto-start ----
    private fun requestAutostart() {
        // Try known OEM autostart activity paths. Fall back to app details.
        val intents = buildAutostartIntents()
        for (intent in intents) {
            try {
                startActivity(intent)
                Toast.makeText(this, R.string.toast_need_manual, Toast.LENGTH_SHORT).show()
                return
            } catch (_: Exception) {
                // try next
            }
        }
        // Fallback: open app details
        Toast.makeText(this, R.string.toast_need_manual, Toast.LENGTH_SHORT).show()
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        })
    }

    private fun buildAutostartIntents(): List<Intent> {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val list = mutableListOf<Intent>()

        when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                list.add(Intent().setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                ))
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                list.add(Intent().setClassName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                ))
            }
            manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> {
                list.add(Intent().setClassName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                ))
                list.add(Intent().setClassName(
                    "com.oplus.safecenter",
                    "com.oplus.safecenter.startupapp.StartupAppListActivity"
                ))
            }
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> {
                list.add(Intent().setClassName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                ))
            }
            manufacturer.contains("samsung") -> {
                list.add(Intent().setClassName(
                    "com.samsung.android.sm",
                    "com.samsung.android.sm.ui.ram.AutoRunActivity"
                ))
            }
            manufacturer.contains("meizu") -> {
                list.add(Intent().setClassName(
                    "com.meizu.safe",
                    "com.meizu.safe.permission.SmartBGActivity"
                ))
            }
        }
        return list
    }

    // ---- Recent tasks lock ----
    private fun openRecentTasks() {
        try {
            // Try to open the system recents screen
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            // On most devices, pressing the recents button is the only way.
            // We collapse the status bar and show a hint instead.
            Toast.makeText(this, R.string.toast_lock_hint, Toast.LENGTH_LONG).show()
            // Try to expand the recent apps panel via StatusBarManager (system-only),
            // otherwise just go home and let the user open recents.
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.toast_lock_hint, Toast.LENGTH_LONG).show()
        }
    }
}
