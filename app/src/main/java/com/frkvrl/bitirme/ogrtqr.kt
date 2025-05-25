package com.frkvrl.bitirme

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.text.SimpleDateFormat
import java.util.*

class ogrtqr : ogrtnavbar() {

    private lateinit var qrCodeImageView: ImageView
    private lateinit var handler: Handler
    private lateinit var lessonCode: String
    private lateinit var sinif: String
    private var runnable: Runnable? = null
    private var attendanceInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ogrtqr)

        lessonCode = intent.getStringExtra("dersID") ?: "BILINMEYEN"
        sinif = intent.getStringExtra("sinif") ?: "BILINMIYOR"
        qrCodeImageView = findViewById(R.id.qrCodeImageView)
        handler = Handler()

        startQRCodeUpdater()

        val viewAttendanceButton: Button = findViewById(R.id.button2)
        viewAttendanceButton.setOnClickListener {
            val intent = Intent(this, yoklamalistesi::class.java)
            intent.putExtra("dersID", lessonCode)
            intent.putExtra("sinif", sinif)
            startActivity(intent)
        }
    }

    private fun startQRCodeUpdater() {
        runnable = object : Runnable {
            override fun run() {
                updateQRCode()

                if (!attendanceInitialized) {
                    initializeAttendanceForToday(lessonCode)
                    attendanceInitialized = true
                }

                handler.postDelayed(this, 5000)
            }
        }
        handler.post(runnable!!)
    }

    private fun updateQRCode() {
        try {
            val token = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            val qrContent = "$token|$timestamp"

            val qrRef = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("qrCodes/current")

            val qrData = mapOf(
                "value" to token,
                "timestamp" to timestamp,
                "lessonCode" to lessonCode
            )

            qrRef.setValue(qrData)

            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(qrContent, com.google.zxing.BarcodeFormat.QR_CODE, 500, 500)
            qrCodeImageView.setImageBitmap(bitmap)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "QR kod oluşturulamadı: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeAttendanceForToday(lessonCode: String) {
        val db = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")

        // Firebase yapınıza göre: dersler/sinif/dersKodu/ogrenciler
        val lessonStudentsRef = db.getReference("dersler/$sinif/$lessonCode/ogrenciler")
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val attendanceRef = db.getReference("attendances/$sinif/$lessonCode/$todayDate")

        lessonStudentsRef.get().addOnSuccessListener { snapshot ->
            val updates = mutableMapOf<String, Any?>()

            // Sadece true değerine sahip öğrencileri al
            for (student in snapshot.children) {
                val value = student.getValue(Boolean::class.java)
                if (value == true) {
                    student.key?.let { updates[it] = false }
                }
            }

            if (updates.isNotEmpty()) {
                attendanceRef.updateChildren(updates).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("ogrtqr", "Yoklama başarıyla sıfırlandı. ${updates.size} öğrenci için.")
                    } else {
                        Log.e("ogrtqr", "Yoklama sıfırlama başarısız: ${task.exception}")
                    }
                }
            } else {
                Log.d("ogrtqr", "Güncellenecek öğrenci bulunamadı.")
            }
        }.addOnFailureListener {
            Log.e("ogrtqr", "Öğrenciler alınamadı: ${it.message}")
        }
    }
}
