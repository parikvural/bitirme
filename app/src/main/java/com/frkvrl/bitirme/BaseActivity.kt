package com.frkvrl.bitirme

import android.content.Intent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

// BaseActivity.kt
open class BaseActivity : AppCompatActivity() {
    override fun setContentView(layoutResID: Int) {
        val fullView = layoutInflater.inflate(R.layout.activity_base, null)
        val activityContainer: FrameLayout = fullView.findViewById(R.id.activity_content)
        layoutInflater.inflate(layoutResID, activityContainer, true)
        super.setContentView(fullView)

        setupNavbar()
    }

    private fun setupNavbar() {
        findViewById<Button>(R.id.nav_home)?.setOnClickListener {
            if (this !is MainActivity2) {
                startActivity(Intent(this, MainActivity2::class.java))
            }
        }

        findViewById<ImageButton>(R.id.nav_qr)?.setOnClickListener {
            if (this !is MainActivity3) {
                startActivity(Intent(this, MainActivity3::class.java))
            }
        }

        findViewById<Button>(R.id.nav_settings)?.setOnClickListener {
            if (this !is MainActivity4) {
                startActivity(Intent(this, MainActivity4::class.java))
            }
        }
    }
}