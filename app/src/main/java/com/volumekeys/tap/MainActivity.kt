package com.volumekeys.tap

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.volumekeys.tap.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private var pickingSide: String? = null
    private val feedbackLines = mutableListOf<String>()

    private val keyEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_KEY_EVENT) return
            val key = intent.getStringExtra("key") ?: return
            val type = intent.getStringExtra("type") ?: return
            val time = intent.getLongExtra("time", System.currentTimeMillis())
            appendFeedback(time, key, type)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)

        setupSection(Config.SIDE_UP, b.upActionSpinner, b.upAppRow, b.upAppName,
            b.upShellRow, b.upShellEdit, b.upAudioRow, b.upAudioSpinner)

        setupSection(Config.SIDE_DOWN, b.downActionSpinner, b.downAppRow, b.downAppName,
            b.downShellRow, b.downShellEdit, b.downAudioRow, b.downAudioSpinner)

        b.upPickApp.setOnClickListener {
            pickingSide = Config.SIDE_UP
            @Suppress("DEPRECATION")
            startActivityForResult(Intent(this, AppPickerActivity::class.java), REQ_PICK_APP)
        }
        b.downPickApp.setOnClickListener {
            pickingSide = Config.SIDE_DOWN
            @Suppress("DEPRECATION")
            startActivityForResult(Intent(this, AppPickerActivity::class.java), REQ_PICK_APP)
        }

        b.upShellTest.setOnClickListener {
            ShellExecutor.runAndToast(this, b.upShellEdit.text.toString())
        }
        b.downShellTest.setOnClickListener {
            ShellExecutor.runAndToast(this, b.downShellEdit.text.toString())
        }

        setupIntervalControls()
        setupFeedbackControls()
        setupAccessibilityStatus()

        b.btnKeepAlive.setOnClickListener {
            startActivity(Intent(this, KeepAliveActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupAccessibilityStatus() {
        b.btnOpenAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        updateServiceStatus()
    }

    private fun updateServiceStatus() {
        val enabled = KeyAccessibilityService.isEnabled(this)
        if (enabled) {
            b.serviceStatusText.text = getString(R.string.service_enabled)
            b.serviceStatusText.setTextColor(
                ContextCompat.getColor(this, R.color.tertiary)
            )
            b.serviceStatusHint.text = getString(R.string.service_enabled_hint)
            b.btnOpenAccessibility.visibility = View.GONE
        } else {
            b.serviceStatusText.text = getString(R.string.service_disabled)
            b.serviceStatusText.setTextColor(
                ContextCompat.getColor(this, R.color.error)
            )
            b.serviceStatusHint.text = getString(R.string.service_disabled_hint)
            b.btnOpenAccessibility.visibility = View.VISIBLE
        }
    }

    private fun setupIntervalControls() {
        b.intervalEdit.setText(Config.getClickIntervalMs(this).toString())

        b.btnSaveInterval.setOnClickListener {
            val text = b.intervalEdit.text.toString().trim()
            val ms = text.toLongOrNull()
            if (ms == null || ms < Config.MIN_CLICK_INTERVAL_MS || ms > Config.MAX_CLICK_INTERVAL_MS) {
                Toast.makeText(this, R.string.toast_interval_invalid, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Config.setClickIntervalMs(this, ms)
            Toast.makeText(
                this,
                getString(R.string.toast_interval_saved, ms),
                Toast.LENGTH_SHORT
            ).show()
        }

        b.btnResetInterval.setOnClickListener {
            Config.resetClickIntervalMs(this)
            val def = Config.DEFAULT_CLICK_INTERVAL_MS
            b.intervalEdit.setText(def.toString())
            Toast.makeText(
                this,
                getString(R.string.toast_interval_reset, def),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupFeedbackControls() {
        b.btnClearFeedback.setOnClickListener {
            feedbackLines.clear()
            b.feedbackLog.text = getString(R.string.feedback_empty)
        }
    }

    private fun appendFeedback(time: Long, key: String, type: String) {
        val keyLabel = if (key == Config.SIDE_UP)
            getString(R.string.feedback_key_up) else getString(R.string.feedback_key_down)
        val msg = if (type == "double")
            getString(R.string.feedback_double, keyLabel)
        else
            getString(R.string.feedback_single, keyLabel)
        val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(time))
        val line = "[$ts] $msg"
        feedbackLines.add(0, line)
        if (feedbackLines.size > 100) feedbackLines.removeAt(feedbackLines.size - 1)
        runOnUiThread {
            b.feedbackLog.text = feedbackLines.joinToString("\n")
            b.feedbackScroll.post { b.feedbackScroll.fullScroll(View.FOCUS_UP) }
        }
    }

    private fun setupSection(
        side: String,
        actionMenu: MaterialAutoCompleteTextView,
        appRow: View, appName: android.widget.TextView,
        shellRow: View, shellEdit: android.widget.EditText,
        audioRow: View, audioMenu: MaterialAutoCompleteTextView
    ) {
        val actionAdapter = ArrayAdapter(
            this, android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.action_types).toList()
        )
        actionMenu.setAdapter(actionAdapter)
        actionMenu.setOnItemClickListener { _, _, position, _ ->
            val action = actionForPosition(position)
            Config.setAction(this@MainActivity, side, action)
            applyActionVisibility(action, appRow, shellRow, audioRow)
        }
        actionMenu.setText(
            actionAdapter.getItem(positionForAction(Config.getAction(this, side))),
            false
        )

        appName.text = Config.getAppName(this, side).ifEmpty { getString(R.string.no_app_selected) }

        shellEdit.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        shellEdit.setText(Config.getShell(this, side))

        val audioAdapter = ArrayAdapter(
            this, android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.audio_modes).toList()
        )
        audioMenu.setAdapter(audioAdapter)
        audioMenu.setOnItemClickListener { _, _, position, _ ->
            val mode = audioModeForPosition(position)
            Config.setAudioMode(this@MainActivity, side, mode)
        }
        audioMenu.setText(
            audioAdapter.getItem(positionForAudioMode(Config.getAudioMode(this, side))),
            false
        )

        applyActionVisibility(
            Config.getAction(this, side), appRow, shellRow, audioRow
        )
    }

    private fun applyActionVisibility(
        action: ActionType, appRow: View, shellRow: View, audioRow: View
    ) {
        appRow.visibility = if (action == ActionType.OPEN_APP) View.VISIBLE else View.GONE
        shellRow.visibility = if (action == ActionType.SHELL) View.VISIBLE else View.GONE
        audioRow.visibility = if (action == ActionType.AUDIO_MODE) View.VISIBLE else View.GONE
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_PICK_APP) return
        val side = pickingSide ?: return
        pickingSide = null
        if (resultCode != Activity.RESULT_OK) return
        val pkg = data?.getStringExtra(AppPickerActivity.EXTRA_PACKAGE) ?: return
        val name = data.getStringExtra(AppPickerActivity.EXTRA_LABEL) ?: pkg
        Config.setApp(this, side, pkg, name)
        val tv = if (side == Config.SIDE_UP) b.upAppName else b.downAppName
        tv.setText(name)
        Toast.makeText(this, getString(R.string.toast_app_selected, name), Toast.LENGTH_SHORT).show()
    }

    override fun onStop() {
        super.onStop()
        Config.setShell(this, Config.SIDE_UP, b.upShellEdit.text.toString())
        Config.setShell(this, Config.SIDE_DOWN, b.downShellEdit.text.toString())
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
        val filter = IntentFilter(ACTION_KEY_EVENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(keyEventReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(keyEventReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        runCatching { unregisterReceiver(keyEventReceiver) }
    }

    private fun actionForPosition(p: Int): ActionType = when (p) {
        0 -> ActionType.NONE
        1 -> ActionType.OPEN_APP
        2 -> ActionType.SHELL
        3 -> ActionType.AUDIO_MODE
        else -> ActionType.NONE
    }
    private fun positionForAction(a: ActionType): Int = when (a) {
        ActionType.NONE -> 0
        ActionType.OPEN_APP -> 1
        ActionType.SHELL -> 2
        ActionType.AUDIO_MODE -> 3
    }
    // audio_modes array: [静音（小米）, 响铃]
    private fun audioModeForPosition(p: Int): AudioModeAction = when (p) {
        0 -> AudioModeAction.SILENT_XIAOMI
        1 -> AudioModeAction.RING
        else -> AudioModeAction.SILENT_XIAOMI
    }
    private fun positionForAudioMode(m: AudioModeAction): Int = when (m) {
        AudioModeAction.SILENT_XIAOMI -> 0
        AudioModeAction.RING -> 1
    }

    companion object {
        private const val REQ_PICK_APP = 1001
        private const val ACTION_KEY_EVENT = "com.volumekeys.tap.KEY_EVENT"
    }
}
