package com.volumekeys.tap

import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import android.widget.Toast
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import java.io.FileInputStream

/**
 * 执行 Shell 命令，支持 Root (su) 和 Shizuku 两种模式。
 *
 * 由 [ActionExecutor] 和 MainActivity 的"测试运行"按钮调用。
 */
object ShellExecutor {

    data class Result(val exitCode: Int, val output: String)

    /** Shizuku 权限请求码 */
    private const val SHIZUKU_REQUEST_CODE = 100

    /**
     * 检查 Shizuku 是否可用（已安装且正在运行）。
     */
    fun isShizukuAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    /**
     * 检查 Shizuku 权限是否已授予。
     */
    fun isShizukuPermissionGranted(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    /**
     * 请求 Shizuku 权限。
     */
    fun requestShizukuPermission() {
        try {
            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
        } catch (_: Throwable) {
            // Shizuku 未运行或版本过低
        }
    }

    /**
     * 根据 [Config.getShellMode] 选择执行方式。
     */
    fun run(ctx: Context, command: String): Result {
        if (command.isBlank()) return Result(-1, "empty command")
        return when (Config.getShellMode(ctx)) {
            ShellMode.ROOT -> runAsRoot(command)
            ShellMode.SHIZUKU -> runViaShizuku(command)
        }
    }

    /**
     * 使用 su 二进制以 root 权限执行命令。
     */
    fun runAsRoot(command: String): Result {
        if (command.isBlank()) return Result(-1, "empty command")
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val out = process.inputStream.bufferedReader().use { it.readText() }
            val err = process.errorStream.bufferedReader().use { it.readText() }
            val code = process.waitFor()
            Result(code, if (out.isNotBlank()) out else err)
        } catch (t: Throwable) {
            Result(-1, t.localizedMessage ?: t.toString())
        }
    }

    /**
     * 通过 Shizuku API 执行命令。
     */
    fun runViaShizuku(command: String): Result {
        if (command.isBlank()) return Result(-1, "empty command")
        if (!isShizukuAvailable()) return Result(-1, "Shizuku 未运行")
        if (!isShizukuPermissionGranted()) return Result(-1, "Shizuku 权限未授予")
        return try {
            val binder = Shizuku.getBinder()
            val service = IShizukuService.Stub.asInterface(binder)
            val remoteProcess = service.newProcess(arrayOf("sh", "-c", command), null, null)

            val outFd = remoteProcess.inputStream
            val errFd = remoteProcess.errorStream
            val out = FileInputStream(outFd.fileDescriptor).bufferedReader().use { it.readText() }
            val err = FileInputStream(errFd.fileDescriptor).bufferedReader().use { it.readText() }
            val code = remoteProcess.waitFor()

            outFd.close()
            errFd.close()

            Result(code, if (out.isNotBlank()) out else err)
        } catch (t: Throwable) {
            Result(-1, t.localizedMessage ?: t.toString())
        }
    }

    /** 便捷方法：执行命令并弹出 Toast 提示结果。 */
    fun runAndToast(ctx: Context, command: String) {
        Thread {
            val r = run(ctx, command)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(
                    ctx,
                    "exit=${r.exitCode}\n${r.output.take(200)}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.start()
    }
}
