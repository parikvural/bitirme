package com.frkvrl.bitirme

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.text.SimpleDateFormat
import java.util.*

class ogrtdevbilgi : ogrtnavbar() {

    private lateinit var qrCodeImageView: ImageView
    private lateinit var handler: Handler
    private lateinit var lessonCode: String
    private var runnable: Runnable? = null
    private var attendanceInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ogrtdevbilgi)

        lessonCode = intent.getStringExtra("dersID") ?: "BILINMEYEN"

        qrCodeImageView = findViewById(R.id.qrCodeImageView)
        handler = Handler()

        startQRCodeUpdater()
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

            val database = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")
            val qrRef = database.getReference("qrCodes/current")

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
        val database = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")
        val lessonStudentsRef = database.getReference("lessons/$lessonCode/ogrenciler")
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        lessonStudentsRef.get().addOnSuccessListener { snapshot ->
            val attendanceRef = database.getReference("attendances/$lessonCode/$todayDate")
            val updates = mutableMapOf<String, Any?>()

            for (student in snapshot.children) {
                val uid = student.key
                if (uid != null) {
                    updates[uid] = false
                }
            }

            attendanceRef.updateChildren(updates).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("ogrtdevbilgi", "Yoklama başarıyla sıfırlandı.")
                } else {
                    Log.e("ogrtdevbilgi", "Yoklama sıfırlama başarısız: ${task.exception}")
                }
            }
        }.addOnFailureListener {
            Log.e("ogrtdevbilgi", "Öğrenciler alınamadı: ${it.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runnable?.let { handler.removeCallbacks(it) }
    }
}
