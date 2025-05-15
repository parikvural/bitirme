package com.frkvrl.bitirme

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.util.*

class ogrtqr : ogrtnavbar() {
    private lateinit var qrCodeImageView: ImageView
    private lateinit var handler: Handler
    private lateinit var timer: Timer
    private lateinit var lessonCode: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ogrtqr)



        val dersID = intent.getStringExtra("dersID") ?: "BILINMEYEN"
        lessonCode = dersID

        val myButton = findViewById<Button>(R.id.button5)

        myButton.setOnClickListener {
            val intent = Intent(this, ogrtdevbilgi::class.java)
            startActivity(intent)
        }

        qrCodeImageView = findViewById(R.id.qrCodeImageView)

        handler = Handler()
        timer = Timer()

        // QR kodunu her 5 saniyede bir değiştirecek şekilde Timer kullan
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // QR kodunu güncelle
                handler.post {
                    updateQRCode()
                }
            }
        }, 0, 5000) // İlk başlatmadan hemen başlayacak ve her 5 saniyede bir tekrar edecek

    }

    private fun updateQRCode() {
        try {
            val token = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            val qrContent = "$token|$timestamp"


            // Firebase'e yaz
            val database = FirebaseDatabase.getInstance()
            val qrRef = database.getReference("qrCodes/current")

            // QR kodu oluşturmak için
            val qrData = mapOf(
                "value" to token,
                "timestamp" to timestamp,
                "used" to false,
                "lessonCode" to lessonCode // artık dinamik!
            )



            qrRef.setValue(qrData)

            // QR kodu oluştur ve göster
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(qrContent, BarcodeFormat.QR_CODE, 500, 500)
            qrCodeImageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }




    override fun onDestroy() {
        super.onDestroy()
        // Timer'ı durdur
        timer.cancel()
    }
}