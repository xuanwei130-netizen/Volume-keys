package com.volumekeys.tap

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.volumekeys.tap.databinding.ActivityAppPickerBinding

/**
 * Lets the user pick an installed, launchable app.
 *
 * Layout: a search EditText pinned to the top, a RecyclerView below. Typing
 * filters the list by app name OR package name (case-insensitive).
 *
 * Returns the selected app via [EXTRA_PACKAGE] / [EXTRA_LABEL].
 */
class AppPickerActivity : AppCompatActivity() {

    private lateinit var b: ActivityAppPickerBinding
    private val adapter = AppListAdapter { app -> returnSelected(app) }

    private var allApps: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAppPickerBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        b.list.adapter = adapter
        b.list.layoutManager = LinearLayoutManager(this).apply {
            initialPrefetchItemCount = 12
        }

        b.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = applyFilter(s?.toString())
        })

        b.cancel.setOnClickListener { finish() }

        loadAppsAsync()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadAppsAsync() {
        Thread {
            val pm = packageManager
            val flags = PackageManager.GET_META_DATA
            val installed = runCatching {
                pm.getInstalledApplications(flags)
            }.getOrDefault(emptyList())
            val apps = installed
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .map {
                    AppInfo(
                        packageName = it.packageName,
                        label = pm.getApplicationLabel(it).toString(),
                        icon = runCatching { pm.getApplicationIcon(it) }.getOrNull(),
                    )
                }
                .sortedBy { it.label.lowercase() }
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                allApps = apps
                applyFilter(b.search.text?.toString())
            }
        }.start()
    }

    private fun applyFilter(query: String?) {
        val q = query?.trim()?.lowercase().orEmpty()
        val filtered = if (q.isEmpty()) allApps
                       else allApps.filter {
                           it.label.lowercase().contains(q) ||
                           it.packageName.lowercase().contains(q)
                       }
        adapter.submitList(filtered)
        b.emptyHint.visibility =
            if (filtered.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun returnSelected(app: AppInfo) {
        val data = Intent().apply {
            putExtra(EXTRA_PACKAGE, app.packageName)
            putExtra(EXTRA_LABEL, app.label)
        }
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_LABEL = "extra_label"
    }
}
