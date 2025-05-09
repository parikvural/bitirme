package com.frkvrl.bitirme

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ogrbilgi : ogrnavbar() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.ogrbilgi)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    fun devam(view:View)
    {
        val intent = Intent(this,ogrdevam::class.java)
        startActivity(intent)
    }
    fun ders(view:View)
    {
        val intent = Intent(this,ogrders::class.java)
        startActivity(intent)
    }
}