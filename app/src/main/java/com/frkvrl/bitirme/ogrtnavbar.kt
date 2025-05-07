package com.frkvrl.bitirme

import android.content.Intent
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

open class ogrtnavbar : AppCompatActivity() {
    override fun setContentView(layoutResID: Int) {
        val fullView = layoutInflater.inflate(R.layout.ogrtnavbar_1, null)
        val activityContainer: FrameLayout = fullView.findViewById(R.id.activity_content)
        layoutInflater.inflate(layoutResID, activityContainer, true)
        super.setContentView(fullView)

        setupNavbar()
    }

    private fun setupNavbar() {
        findViewById<Button>(R.id.nav_ogrtana)?.setOnClickListener {
            if (this !is ogrtana) {
                startActivity(Intent(this, ogrtana::class.java))
            }
        }

        findViewById<Button>(R.id.nav_ogrtders)?.setOnClickListener {
            if (this !is ogrtders) {
                startActivity(Intent(this, ogrtders::class.java))
            }
        }


        }
    }
