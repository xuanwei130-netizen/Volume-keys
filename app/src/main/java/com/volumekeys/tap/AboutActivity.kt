package com.volumekeys.tap

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.volumekeys.tap.databinding.ActivityAboutBinding

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
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coolapk.com"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { startActivity(intent) }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
