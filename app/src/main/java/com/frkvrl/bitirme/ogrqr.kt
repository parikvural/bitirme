package com.frkvrl.bitirme

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.integration.android.IntentIntegrator
import com.google.firebase.database.FirebaseDatabase
import java.util.Date
import java.util.Locale

class ogrqr : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Direkt olarak QR taramayı başlat
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
                // Tarama sonucu alındı
                validateQRCode(result.contents)
            } else {
                Toast.makeText(this, "QR kod okunamadı", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateQRCode(scannedContent: String) {
        val parts = scannedContent.split("|")
        if (parts.size != 2) {
            Toast.makeText(this, "Hatalı QR formatı", Toast.LENGTH_SHORT).show()
            return
        }

        val scannedToken = parts[0]
        val scannedTimestamp = parts[1].toLongOrNull() ?: return
        val currentTime = System.currentTimeMillis()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val qrRef = FirebaseDatabase.getInstance().getReference("qrCodes/current")
        qrRef.get().addOnSuccessListener { snapshot ->
            val value = snapshot.child("value").getValue(String::class.java)
            val timestamp = snapshot.child("timestamp").getValue(Long::class.java)
            val used = snapshot.child("used").getValue(Boolean::class.java) ?: false
            val lessonCode = snapshot.child("lessonCode").getValue(String::class.java)

            if (value == scannedToken && timestamp != null && !used && lessonCode != null) {
                val isValid = (currentTime - timestamp) <= 5000
                if (isValid) {
                    qrRef.child("used").setValue(true)

                    // Yoklama verisini kaydet
                    val date = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                        Date()
                    )
                    val attendanceRef = FirebaseDatabase.getInstance()
                        .getReference("attendances/$lessonCode/$date/$uid")

                    attendanceRef.setValue(true)

                    Toast.makeText(this, "Yoklama alındı ✅", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Kod süresi dolmuş ⏰", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Geçersiz QR kod ❌", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Firebase hatası", Toast.LENGTH_SHORT).show()
        }
    }
}
