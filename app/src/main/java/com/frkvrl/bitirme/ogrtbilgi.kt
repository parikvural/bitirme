package com.frkvrl.bitirme

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class ogrtbilgi : ogrtnavbar() {

    private lateinit var lessonCode: String
    private lateinit var selectedDate: String
    private lateinit var sinif: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ogrtbilgi)

        try {
            lessonCode = intent.getStringExtra("dersID") ?: "BILINMEYEN"
            selectedDate = intent.getStringExtra("secilenTarih") ?: getTodayDate()
            sinif = intent.getStringExtra("sinif") ?: "0"

            Log.d("ogrtbilgi", "onCreate - lessonCode: $lessonCode, selectedDate: $selectedDate, sinif: $sinif")
        } catch (e: Exception) {
            Log.e("ogrtbilgi", "Hata oluştu: ${e.message}", e)
            Toast.makeText(this, "Başlatma hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }

        findViewById<Button>(R.id.btnDatePicker).setOnClickListener {
            showDatePickerDialog()
        }

        findViewById<Button>(R.id.button5).visibility = View.GONE

        findViewById<Button>(R.id.button2).setOnClickListener {
            val intent = Intent(this, DevamsizlikListesiActivity::class.java)
            intent.putExtra("dersID", lessonCode)
            intent.putExtra("sinif", sinif)
            startActivity(intent)
        }
        findViewById<Button>(R.id.btnQrBaslat).setOnClickListener {
            startQrSession()
        }


    }

    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this,
            { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                Toast.makeText(this, "Seçilen tarih: $selectedDate", Toast.LENGTH_SHORT).show()

                // Yalnızca yoklama verisini oluştur
                createAttendanceIfNotExists()
            }, year, month, day)

        datePickerDialog.show()
    }


    private fun createAttendanceIfNotExists() {
        val database = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app")
        val attendanceRef = database.getReference("attendances/$sinif/$lessonCode/$selectedDate")

        attendanceRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                Toast.makeText(this, "Yoklama zaten başlatılmış.", Toast.LENGTH_SHORT).show()
            } else {
                val lessonsRef = database.getReference("dersler")
                lessonsRef.get().addOnSuccessListener { derslerSnapshot ->
                    var found = false
                    for (dersGroup in derslerSnapshot.children) {
                        val dersNode = dersGroup.child(lessonCode)
                        if (dersNode.exists()) {
                            val ogrencilerNode = dersNode.child("ogrenciler")
                            val attendanceData = mutableMapOf<String, Boolean>()

                            for (ogrenci in ogrencilerNode.children) {
                                ogrenci.key?.let { uid -> attendanceData[uid] = false }
                            }

                            if (attendanceData.isEmpty()) {
                                Toast.makeText(this, "Derse kayıtlı öğrenci yok", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            attendanceRef.setValue(attendanceData).addOnSuccessListener {
                                Toast.makeText(this, "Yoklama oluşturuldu ✅", Toast.LENGTH_SHORT).show()
                            }.addOnFailureListener {
                                Toast.makeText(this, "Yoklama başlatılamadı ❌", Toast.LENGTH_SHORT).show()
                            }

                            found = true
                            break
                        }
                    }

                    if (!found) {
                        Toast.makeText(this, "Ders bulunamadı: $lessonCode", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Ders bilgisi alınamadı", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Veri alınamadı: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun startQrSession() {
        val database = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app")
        val qrRef = database.getReference("qrCodes/current")

        val qrData = mapOf(
            "value" to UUID.randomUUID().toString().take(8),
            "timestamp" to System.currentTimeMillis(),
            "lessonCode" to lessonCode,
            "sinif" to sinif
        )

        qrRef.setValue(qrData).addOnSuccessListener {
            Toast.makeText(this, "QR oturumu başlatıldı", Toast.LENGTH_SHORT).show()
            goToStudentInfo()
        }.addOnFailureListener {
            Toast.makeText(this, "QR oturumu başlatılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goToStudentInfo() {
        val intent = Intent(this, ogrtqr::class.java)
        intent.putExtra("secilenTarih", selectedDate)
        intent.putExtra("dersID", lessonCode)
        intent.putExtra("sinif", sinif)
        startActivity(intent)
    }
}
