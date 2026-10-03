package com.volumekeys.tap

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * AccessibilityService that intercepts volume key presses.
 *
 * This replaces the previous LSPosed approach. Advantages:
 *  - Works on ALL ROMs (AOSP / HyperOS / ColorOS) without class-name guesswork
 *  - No root required for app-launch / ringer-mode actions
 *  - Runs in the app process, so config is read directly from SharedPreferences
 *
 * The service is started by the system when the user enables it in
 * Settings -> Accessibility. It filters key events and detects double-clicks.
 */
class KeyAccessibilityService : AccessibilityService() {

    private var lastUpTime = 0L
    private var lastDownTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility service connected")
        // No extra config needed: flags are set in accessibility_service_config.xml
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for key handling; kept for completeness.
    }

    override fun onInterrupt() {
        Log.i(TAG, "Accessibility service interrupted")
    }

    /**
     * Called for every key event when flagRequestFilterKeyEvents is set.
     * Return true to consume the event (block default handling), false to pass it
     * through to the rest of the system.
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        try {
            if (event.action != KeyEvent.ACTION_DOWN) return false

            val keyCode = event.keyCode
            val isUp = keyCode == KeyEvent.KEYCODE_VOLUME_UP
            val isDown = keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
            if (!isUp && !isDown) return false

            val now = SystemClock.uptimeMillis()
            val lastTime = if (isUp) lastUpTime else lastDownTime
            val interval = Config.getClickIntervalMs(this)

            if (lastTime != 0L && (now - lastTime) <= interval) {
                // Double-click: consume the event and run the action.
                if (isUp) lastUpTime = 0L else lastDownTime = 0L
                val side = if (isUp) Config.SIDE_UP else Config.SIDE_DOWN
                Log.i(TAG, "Double-click detected: $side")
                ActionExecutor.execute(this, side)
                sendFeedback(isUp, TYPE_DOUBLE)
                return true
            } else {
                // Single click: let the system handle volume normally.
                if (isUp) lastUpTime = now else lastDownTime = now
                sendFeedback(isUp, TYPE_SINGLE)
                return false
            }
        } catch (t: Throwable) {
            Log.e(TAG, "onKeyEvent failed", t)
            return false
        }
    }

    private fun sendFeedback(isUp: Boolean, type: String) {
        try {
            val intent = Intent(ACTION_KEY_EVENT).apply {
                setPackage(packageName)
                putExtra("key", if (isUp) Config.SIDE_UP else Config.SIDE_DOWN)
                putExtra("type", type)
                putExtra("time", System.currentTimeMillis())
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            sendBroadcast(intent)
        } catch (t: Throwable) {
            Log.e(TAG, "sendFeedback failed", t)
        }
    }

    companion object {
        private const val TAG = "VolumeKeys"
        const val ACTION_KEY_EVENT = "com.volumekeys.tap.KEY_EVENT"
        const val TYPE_SINGLE = "single"
        const val TYPE_DOUBLE = "double"

        /**
         * Checks whether this accessibility service is currently enabled.
         * Used by MainActivity to show/hide the setup prompt.
         */
        fun isEnabled(ctx: Context): Boolean {
            val enabled = android.provider.Settings.Secure.getString(
                ctx.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabled.contains("${ctx.packageName}/${KeyAccessibilityService::class.java.name}")
        }
    }
}
