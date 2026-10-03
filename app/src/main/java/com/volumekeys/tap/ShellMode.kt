package com.volumekeys.tap

/**
 * Shell 执行模式。
 * - ROOT: 使用 su 二进制（需设备已 root）
 * - SHIZUKU: 通过 Shizuku API 执行（需安装并激活 Shizuku）
 */
enum class ShellMode(val value: String) {
    ROOT("root"),
    SHIZUKU("shizuku");

    companion object {
        fun fromValue(v: String?): ShellMode =
            entries.firstOrNull { it.value == v } ?: ROOT
    }
}
