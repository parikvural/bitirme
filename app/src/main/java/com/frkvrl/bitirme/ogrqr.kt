package com.frkvrl.bitirme

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.integration.android.IntentIntegrator
import java.util.*

class ogrqr : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("QR kodu okutunuz")
        integrator.setBeepEnabled(true)
        integrator.setOrientationLocked(true)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                validateQRCode(result.contents)
            } else {
                Toast.makeText(this, "QR kod okunamadı", Toast.LENGTH_SHORT).show()
                finish() // okutma başarısızsa da ekranı kapatabiliriz
            }
        }
    }

    private fun validateQRCode(scannedContent: String) {
        val parts = scannedContent.split("|")
        if (parts.size != 2) {
            Toast.makeText(this, "Hatalı QR formatı", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val scannedToken = parts[0]
        val scannedTimestamp = parts[1].toLongOrNull() ?: run {
            Toast.makeText(this, "Hatalı zaman damgası", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val currentTime = System.currentTimeMillis()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(this, "Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val database = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")
        val qrRef = database.getReference("qrCodes/current")
        qrRef.get().addOnSuccessListener { snapshot ->
            val value = snapshot.child("value").getValue(String::class.java)
            val timestamp = snapshot.child("timestamp").getValue(Long::class.java)
            val lessonCode = snapshot.child("lessonCode").getValue(String::class.java)

            if (value == scannedToken && timestamp != null && lessonCode != null) {
                val isValid = (currentTime - timestamp) <= 5000 // 5 saniye geçerlilik
                if (isValid) {
                    val attendanceRef = database.getReference("attendances/$lessonCode/${getCurrentDate()}/$uid")

                    attendanceRef.get().addOnSuccessListener { attendanceSnapshot ->
                        val currentValue = attendanceSnapshot.getValue(Boolean::class.java)
                        if (currentValue == true) {
                            Toast.makeText(this, "Zaten yoklama alınmış ✅", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            attendanceRef.setValue(true).addOnSuccessListener {
                                Toast.makeText(this, "Yoklama alındı ✅", Toast.LENGTH_SHORT).show()
                                finish()
                            }.addOnFailureListener {
                                Toast.makeText(this, "Yoklama kaydı başarısız", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    }.addOnFailureListener {
                        Toast.makeText(this, "Yoklama kontrolü başarısız", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Kod süresi dolmuş ⏰", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else {
                Toast.makeText(this, "Geçersiz QR kod ❌", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Firebase hatası", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun getCurrentDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
