package com.volumekeys.tap

import android.content.Context
import android.content.SharedPreferences

/**
 * Centralized read/write of all configuration.
 *
 * Both the settings UI ([MainActivity]) and the key-interception service
 * ([KeyAccessibilityService]) run in the app process, so they share these
 * SharedPreferences directly — no IPC needed.
 */
object Config {

    const val PREFS_NAME = "volkeyhook_config"

    // ---- Keys (per volume key) ----
    private fun keyAction(side: String) = "${side}_action"
    private fun keyAppPackage(side: String) = "${side}_app_package"
    private fun keyAppName(side: String) = "${side}_app_name"
    private fun keyShell(side: String) = "${side}_shell"
    private fun keyAudioMode(side: String) = "${side}_audio_mode"

    // ---- Global settings ----
    private const val KEY_CLICK_INTERVAL_MS = "click_interval_ms"
    private const val KEY_SHELL_MODE = "shell_mode"
    private const val KEY_HIDE_CARD = "hide_card"

    const val DEFAULT_CLICK_INTERVAL_MS = 400L
    const val MIN_CLICK_INTERVAL_MS = 100L
    const val MAX_CLICK_INTERVAL_MS = 2000L

    const val SIDE_UP = "vol_up"
    const val SIDE_DOWN = "vol_down"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---- Read helpers ----
    fun getAction(ctx: Context, side: String): ActionType =
        ActionType.fromValue(prefs(ctx).getString(keyAction(side), null))

    fun getAppPackage(ctx: Context, side: String): String =
        prefs(ctx).getString(keyAppPackage(side), "") ?: ""

    fun getAppName(ctx: Context, side: String): String =
        prefs(ctx).getString(keyAppName(side), "") ?: ""

    fun getShell(ctx: Context, side: String): String =
        prefs(ctx).getString(keyShell(side), "") ?: ""

    fun getAudioMode(ctx: Context, side: String): AudioModeAction =
        AudioModeAction.fromValue(prefs(ctx).getString(keyAudioMode(side), null))

    // ---- Write helpers ----
    private fun edit(ctx: Context) = prefs(ctx).edit()

    fun setAction(ctx: Context, side: String, action: ActionType) {
        edit(ctx).putString(keyAction(side), action.value).apply()
    }

    fun setApp(ctx: Context, side: String, pkg: String, name: String) {
        edit(ctx)
            .putString(keyAppPackage(side), pkg)
            .putString(keyAppName(side), name)
            .apply()
    }

    fun setShell(ctx: Context, side: String, shell: String) {
        edit(ctx).putString(keyShell(side), shell).apply()
    }

    fun setAudioMode(ctx: Context, side: String, mode: AudioModeAction) {
        edit(ctx).putString(keyAudioMode(side), mode.value).apply()
    }

    // ---- Global: double-click detection interval ----
    fun getClickIntervalMs(ctx: Context): Long {
        val raw = prefs(ctx).getLong(KEY_CLICK_INTERVAL_MS, DEFAULT_CLICK_INTERVAL_MS)
        return raw.coerceIn(MIN_CLICK_INTERVAL_MS, MAX_CLICK_INTERVAL_MS)
    }

    fun setClickIntervalMs(ctx: Context, ms: Long) {
        val clamped = ms.coerceIn(MIN_CLICK_INTERVAL_MS, MAX_CLICK_INTERVAL_MS)
        edit(ctx).putLong(KEY_CLICK_INTERVAL_MS, clamped).apply()
    }

    fun resetClickIntervalMs(ctx: Context) {
        edit(ctx).remove(KEY_CLICK_INTERVAL_MS).apply()
    }

    // ---- Global: shell execution mode ----
    fun getShellMode(ctx: Context): ShellMode =
        ShellMode.fromValue(prefs(ctx).getString(KEY_SHELL_MODE, null))

    fun setShellMode(ctx: Context, mode: ShellMode) {
        edit(ctx).putString(KEY_SHELL_MODE, mode.value).apply()
    }

    // ---- Global: hide background card ----
    fun isCardHidden(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_HIDE_CARD, false)

    fun setCardHidden(ctx: Context, hidden: Boolean) {
        edit(ctx).putBoolean(KEY_HIDE_CARD, hidden).apply()
    }
}
