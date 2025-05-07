package com.frkvrl.bitirme

import android.content.Intent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

// ogrnavbar.kt
open class ogrnavbar : AppCompatActivity() {
    override fun setContentView(layoutResID: Int) {
        val fullView = layoutInflater.inflate(R.layout.ogrnavbar_1, null)
        val activityContainer: FrameLayout = fullView.findViewById(R.id.activity_content)
        layoutInflater.inflate(layoutResID, activityContainer, true)
        super.setContentView(fullView)

        setupNavbar()
    }

    private fun setupNavbar() {
        findViewById<Button>(R.id.nav_ogrhome)?.setOnClickListener {
            if (this !is ograna) {
                startActivity(Intent(this, ograna::class.java))
            }
        }

        findViewById<ImageButton>(R.id.nav_qr)?.setOnClickListener {
            if (this !is ogrqr) {
                startActivity(Intent(this, ogrqr::class.java))
            }
        }

        findViewById<Button>(R.id.nav_settings)?.setOnClickListener {
            if (this !is ogrbilgi) {
                startActivity(Intent(this, ogrbilgi::class.java))
            }
        }
    }
}