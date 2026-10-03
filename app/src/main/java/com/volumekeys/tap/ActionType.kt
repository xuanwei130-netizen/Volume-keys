package com.volumekeys.tap

/**
 * The kind of action performed when a volume key is double-clicked.
 */
enum class ActionType(val value: String) {
    NONE("none"),
    OPEN_APP("open_app"),
    SHELL("shell"),
    AUDIO_MODE("audio_mode"); // Set ringer mode to 静音 or 响铃

    companion object {
        fun fromValue(v: String?): ActionType =
            entries.firstOrNull { it.value == v } ?: NONE
    }
}

/**
 * Ringer mode chosen for the AUDIO_MODE action.
 * 震动选项已移除，提供两种模式：
 *  - SILENT_XIAOMI: 用 RINGER_MODE_VIBRATE(1)，在小米/部分国产 ROM 上实际表现为静音
 *  - RING: RINGER_MODE_NORMAL(2)
 */
enum class AudioModeAction(val value: String, val ringerModeConstant: Int) {
    SILENT_XIAOMI("silent_xiaomi", 1),  // AudioManager.RINGER_MODE_VIBRATE
    RING("ring", 2);                    // AudioManager.RINGER_MODE_NORMAL

    companion object {
        fun fromValue(v: String?): AudioModeAction =
            entries.firstOrNull { it.value == v } ?: SILENT_XIAOMI
    }
}
