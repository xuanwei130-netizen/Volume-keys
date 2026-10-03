package com.volumekeys.tap

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.volumekeys.tap.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupShellMode()
        updateShizukuStatus()

        b.btnShizukuAction.setOnClickListener {
            ShellExecutor.requestShizukuPermission()
        }

        b.btnOpenShizuku.setOnClickListener {
            openShizukuApp()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        updateShizukuStatus()
    }

    private fun setupShellMode() {
        val modes = arrayOf(getString(R.string.shell_mode_root), getString(R.string.shell_mode_shizuku))
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, modes.toList())
        b.shellModeMenu.setAdapter(adapter)

        b.shellModeMenu.setOnItemClickListener { _, _, position, _ ->
            val mode = if (position == 0) ShellMode.ROOT else ShellMode.SHIZUKU
            Config.setShellMode(this, mode)
            updateShizukuStatus()
        }

        val currentMode = Config.getShellMode(this)
        val pos = if (currentMode == ShellMode.ROOT) 0 else 1
        b.shellModeMenu.setText(adapter.getItem(pos)!!, false)
    }

    private fun updateShizukuStatus() {
        val mode = Config.getShellMode(this)
        if (mode == ShellMode.SHIZUKU) {
            b.shizukuStatusCard.visibility = View.VISIBLE
            val available = ShellExecutor.isShizukuAvailable()
            if (available) {
                val granted = ShellExecutor.isShizukuPermissionGranted()
                if (granted) {
                    b.shizukuStatusText.text = getString(R.string.shizuku_granted)
                    b.shizukuStatusText.setTextColor(
                        ContextCompat.getColor(this, R.color.tertiary)
                    )
                    b.btnShizukuAction.visibility = View.GONE
                } else {
                    b.shizukuStatusText.text = getString(R.string.shizuku_not_granted)
                    b.shizukuStatusText.setTextColor(
                        ContextCompat.getColor(this, R.color.error)
                    )
                    b.btnShizukuAction.visibility = View.VISIBLE
                    b.btnShizukuAction.text = getString(R.string.btn_shizuku_request)
                }
            } else {
                b.shizukuStatusText.text = getString(R.string.shizuku_not_running)
                b.shizukuStatusText.setTextColor(
                    ContextCompat.getColor(this, R.color.error)
                )
                b.btnShizukuAction.visibility = View.GONE
            }
        } else {
            b.shizukuStatusCard.visibility = View.GONE
        }
    }

    private fun openShizukuApp() {
        val intent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else {
            // Shizuku 未安装，跳转到酷安或官方页面
            val playIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coolapk.com"))
            playIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { startActivity(playIntent) }
        }
    }
}
