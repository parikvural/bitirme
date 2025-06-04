package com.frkvrl.bitirme

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.text.SimpleDateFormat
import java.util.*

// ... [importlar aynı]

class ogrtqr : ogrtnavbar() {

    private lateinit var qrCodeImageView: ImageView
    private lateinit var iptalButton: Button
    private lateinit var handler: Handler
    private lateinit var lessonCode: String
    private lateinit var sinif: String
    private var runnable: Runnable? = null
    private var currentQrToken: String? = null
    private var currentQrTimestamp: Long? = null
    private var attendanceInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ogrtqr)

        lessonCode = intent.getStringExtra("dersID") ?: "BILINMEYEN"
        sinif = intent.getStringExtra("sinif") ?: "BILINMIYOR"

        initializeQrComponentsAndStartUpdater()
    }

    private fun initializeQrComponentsAndStartUpdater() {
        qrCodeImageView = findViewById(R.id.qrCodeImageView)
        iptalButton = findViewById(R.id.buttonIptal)
        handler = Handler()

        startQRCodeUpdater()

        val viewAttendanceButton: Button = findViewById(R.id.button2)
        viewAttendanceButton.setOnClickListener {
            val intent = Intent(this, yoklamalistesi::class.java)
            intent.putExtra("dersID", lessonCode)
            intent.putExtra("sinif", sinif)
            startActivity(intent)
        }

        iptalButton.setOnClickListener {
            val messageTextView = TextView(this).apply {
                text = "Bu dersi iptal etmek istediğinize emin misiniz? Bu işlem yoklama verilerini silecektir."
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@ogrtqr, R.color.onPrimary))
                setPadding(48, 48, 48, 48)
            }

            AlertDialog.Builder(this, R.style.AlertDialogCustom)
                .setTitle("Dersi İptal Et")
                .setView(messageTextView)
                .setPositiveButton("Evet") { _, _ -> iptalDersiVeYoklamayiSil() }
                .setNegativeButton("Hayır", null)
                .show()
                .also { dialog ->
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        ?.setTextColor(ContextCompat.getColor(this, R.color.onPrimary))
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                        ?.setTextColor(ContextCompat.getColor(this, R.color.onPrimary))
                }
        }
    }

    private fun startQRCodeUpdater() {
        runnable = object : Runnable {
            override fun run() {
                updateQRCode()
                handler.postDelayed(this, 15000)
            }
        }
        handler.post(runnable!!)
    }

    private fun updateQRCode() {
        try {
            val token = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val qrContent = "$token|$timestamp|$date"

            val qrRef = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("qrCodes/current")

            val qrData = mapOf(
                "value" to token,
                "timestamp" to timestamp,
                "lessonCode" to lessonCode,
                "sinif" to sinif,
                "date" to date
            )

            qrRef.setValue(qrData)
            currentQrToken = token

            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(qrContent, com.google.zxing.BarcodeFormat.QR_CODE, 500, 500)
            qrCodeImageView.setImageBitmap(bitmap)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "QR kod oluşturulamadı: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    private fun initializeAttendanceForDate(timestamp: Long) {
        val db = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")
        val lessonStudentsRef = db.getReference("dersler/$sinif/$lessonCode/ogrenciler")
        val date = getDateFromTimestamp(timestamp)
        val attendanceRef = db.getReference("attendances/$sinif/$lessonCode/$date")

        attendanceRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Log.d("ogrtqr", "O tarihte yoklama zaten başlatılmış.")
                    return
                }

                lessonStudentsRef.get().addOnSuccessListener { studentsSnapshot ->
                    val updates = mutableMapOf<String, Any?>()
                    for (student in studentsSnapshot.children) {
                        if (student.getValue(Boolean::class.java) == true) {
                            student.key?.let { updates[it] = false }
                        }
                    }

                    if (updates.isNotEmpty()) {
                        attendanceRef.updateChildren(updates).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("ogrtqr", "Yoklama başarıyla başlatıldı.")
                            } else {
                                Log.e("ogrtqr", "Yoklama başlatılamadı: ${task.exception}")
                            }
                        }
                    }
                }.addOnFailureListener {
                    Log.e("ogrtqr", "Öğrenci listesi alınamadı: ${it.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ogrtqr", "Yoklama kontrolü iptal edildi: ${error.message}")
            }
        })
    }

    private fun iptalDersiVeYoklamayiSil() {
        val db = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")
        val date = currentQrTimestamp?.let { getDateFromTimestamp(it) }
            ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) // yedek tarih

        db.getReference("qrCodes/current").removeValue().addOnSuccessListener {
            Log.d("ogrtqr", "QR kod silindi.")
            db.getReference("attendances/$sinif/$lessonCode/$date").removeValue().addOnSuccessListener {
                Toast.makeText(this, "Ders iptal edildi ve yoklama silindi.", Toast.LENGTH_LONG).show()
                finish()
            }.addOnFailureListener { error ->
                Toast.makeText(this, "Yoklama silinemedi: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener { error ->
            Toast.makeText(this, "QR kod silinemedi: ${error.message}", Toast.LENGTH_LONG).show()
        }

        runnable?.let { handler.removeCallbacks(it) }
    }

    private fun getDateFromTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    override fun onDestroy() {
        super.onDestroy()
        runnable?.let { handler.removeCallbacks(it) }
    }
}

