package com.volumekeys.tap

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.volumekeys.tap.databinding.ActivityAboutBinding
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class AboutActivity : AppCompatActivity() {

    private lateinit var b: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val versionName = packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        b.versionText.text = getString(R.string.version_format, versionName)

        b.cardSource.setOnClickListener {
            openUrl("https://github.com/xuanwei130-netizen/Volume-keys")
        }

        b.cardCheckUpdate.setOnClickListener {
            checkUpdate(versionName)
        }

        b.cardFindMe.setOnClickListener {
            openUrl("https://www.coolapk.com/u/34727959")
        }
    }

    private fun checkUpdate(currentVersion: String) {
        b.updateStatus.text = getString(R.string.update_checking)
        b.updateArrow.visibility = android.view.View.GONE

        thread {
            try {
                val url = URL("https://api.github.com/repos/xuanwei130-netizen/Volume-keys/releases/latest")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github+json")
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                conn.inputStream.bufferedReader().use { reader ->
                    val json = JSONObject(reader.readText())
                    val latestTag = json.optString("tag_name", "")
                    val htmlUrl = json.optString("html_url", "")
                    val releaseName = json.optString("name", latestTag)

                    runOnUiThread {
                        if (latestTag.isEmpty()) {
                            b.updateStatus.text = getString(R.string.update_failed)
                            return@runOnUiThread
                        }

                        if (isNewerVersion(latestTag, currentVersion)) {
                            b.updateStatus.text = getString(R.string.update_found, releaseName)
                            b.updateArrow.visibility = android.view.View.VISIBLE
                            b.cardCheckUpdate.setOnClickListener {
                                openUrl(htmlUrl)
                            }
                        } else {
                            b.updateStatus.text = getString(R.string.update_latest)
                            b.updateArrow.visibility = android.view.View.GONE
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    b.updateStatus.text = getString(R.string.update_failed)
                }
            }
        }
    }

    private fun isNewerVersion(latestTag: String, currentVersion: String): Boolean {
        val latest = latestTag.removePrefix("v").lowercase()
        val current = currentVersion.lowercase()
        return latest != current
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
