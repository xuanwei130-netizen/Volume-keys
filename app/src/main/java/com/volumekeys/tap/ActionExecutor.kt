package com.volumekeys.tap

import android.content.Context
import android.content.Intent
import android.media.AudioManager

/**
 * Executes the configured action for a given volume-key side.
 *
 * Runs inside the app process (from KeyAccessibilityService), so it reads
 * configuration directly from SharedPreferences via [Config] — no IPC needed.
 */
object ActionExecutor {

    fun execute(ctx: Context, side: String) {
        try {
            when (Config.getAction(ctx, side)) {
                ActionType.NONE -> Unit
                ActionType.OPEN_APP -> launchApp(ctx, Config.getAppPackage(ctx, side))
                ActionType.SHELL -> ShellExecutor.run(ctx, Config.getShell(ctx, side))
                ActionType.AUDIO_MODE -> setRingerMode(
                    ctx,
                    Config.getAudioMode(ctx, side)
                )
            }
        } catch (t: Throwable) {
            android.util.Log.e("VolKeyHook", "ActionExecutor.execute($side) failed", t)
        }
    }

    private fun launchApp(ctx: Context, pkg: String) {
        if (pkg.isEmpty()) return
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg)
            ?: Intent(Intent.ACTION_MAIN).apply {
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(intent) }
            .onFailure { android.util.Log.e("VolKeyHook", "launchApp($pkg) failed", it) }
    }

    /**
     * Sets the ringer mode.
     *
     *  - SILENT_XIAOMI: setRingerMode(VIBRATE) + zero ring volume
     *  - RING: setRingerMode(NORMAL) + restore ring volume if zero
     */
    private fun setRingerMode(ctx: Context, mode: AudioModeAction) {
        runCatching {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            when (mode) {
                AudioModeAction.SILENT_XIAOMI -> {
                    am.setRingerMode(mode.ringerModeConstant)
                    am.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                }
                AudioModeAction.RING -> {
                    am.setRingerMode(mode.ringerModeConstant)
                    if (am.getStreamVolume(AudioManager.STREAM_RING) == 0) {
                        val max = am.getStreamMaxVolume(AudioManager.STREAM_RING)
                        am.setStreamVolume(AudioManager.STREAM_RING, (max / 2).coerceAtLeast(1), 0)
                    }
                }
            }
            android.util.Log.i("VolKeyHook", "setRingerMode -> $mode")
        }.onFailure { android.util.Log.e("VolKeyHook", "setRingerMode($mode) failed", it) }
    }
}
