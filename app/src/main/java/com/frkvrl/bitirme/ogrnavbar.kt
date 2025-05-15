package com.frkvrl.bitirme

import android.content.Intent
import android.media.Image
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
        findViewById<ImageButton>(R.id.nav_ogrtana)?.setOnClickListener {
            if (this !is ograna) {
                startActivity(Intent(this, ograna::class.java))
            }
        }


        findViewById<ImageButton>(R.id.ogrtders)?.setOnClickListener {
            val intent = Intent(this, ogrqr::class.java)
            startActivity(intent)
        }





        findViewById<ImageButton>(R.id.nav_settings)?.setOnClickListener {
            if (this !is ogrbilgi) {
                startActivity(Intent(this, ogrbilgi::class.java))
            }
        }
    }
}
