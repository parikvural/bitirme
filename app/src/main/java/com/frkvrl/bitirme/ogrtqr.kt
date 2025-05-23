package com.frkvrl.bitirme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*
import android.app.DatePickerDialog
import android.view.View
import android.widget.DatePicker

class ogrtqr : ogrtnavbar() {

    private lateinit var lessonCode: String
    private lateinit var selectedDate: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ogrtqr)

        try {
            lessonCode = intent.getStringExtra("dersID") ?: "BILINMEYEN"
            selectedDate = intent.getStringExtra("secilenTarih") ?: getTodayDate()
            Log.d("ogrtqr", "onCreate - lessonCode: $lessonCode, selectedDate: $selectedDate")
        } catch (e: Exception) {
            Log.e("ogrtqr", "Hata oluştu: ${e.message}", e)
            Toast.makeText(this, "Başlatma hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }

        val btnDatePicker = findViewById<Button>(R.id.btnDatePicker)
        btnDatePicker.setOnClickListener {
            Log.d("ogrtqr", "Date picker butonuna tıklandı")
            showDatePickerDialog()
        }

        findViewById<Button>(R.id.button5).visibility = View.GONE

        val showAllAbsencesButton = findViewById<Button>(R.id.button2)
        showAllAbsencesButton.setOnClickListener {
            val intent = Intent(this, DevamsizlikListesiActivity::class.java)
            intent.putExtra("dersID", lessonCode)
            startActivity(intent)
        }
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
                createAttendanceIfNotExistsAndProceed()
            }, year, month, day)

        datePickerDialog.show()
    }

    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun createAttendanceIfNotExistsAndProceed() {
        val database = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app")
        val attendanceRef = database.getReference("attendances").child(lessonCode).child(selectedDate)

        attendanceRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                Toast.makeText(this, "Yoklama zaten mevcut, kontrol ediliyor...", Toast.LENGTH_SHORT).show()
                checkIfDayIsEmptyAndProceed()
            } else {
                val lessonStudentsRef = database.getReference("lessons").child(lessonCode).child("ogrenciler")
                lessonStudentsRef.get().addOnSuccessListener { studentsSnapshot ->
                    if (studentsSnapshot.exists()) {
                        val attendanceData = mutableMapOf<String, Boolean>()
                        for (student in studentsSnapshot.children) {
                            val studentId = student.key ?: continue
                            attendanceData[studentId] = false
                        }
                        attendanceRef.setValue(attendanceData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Yoklama başarıyla oluşturuldu.", Toast.LENGTH_SHORT).show()
                                checkIfDayIsEmptyAndProceed()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Yoklama oluşturulamadı: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(this, "Dersin öğrenci listesi bulunamadı!", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Ders öğrencileri alınamadı: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Veri alınamadı: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkIfDayIsEmptyAndProceed() {
        val database = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app")
        val dateRef = database.getReference("attendances").child(lessonCode).child(selectedDate)

        dateRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                var allFalse = true
                for (child in snapshot.children) {
                    val value = child.getValue(Boolean::class.java) ?: false
                    if (value) {
                        allFalse = false
                        break
                    }
                }
                if (allFalse) {
                    Toast.makeText(this, "Yoklama boş, devam ediliyor...", Toast.LENGTH_SHORT).show()
                    goToStudentInfo()
                } else {
                    Toast.makeText(this, "Bu tarihte zaten yoklama alınmış.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Yoklama verisi yok, devam ediliyor...", Toast.LENGTH_SHORT).show()
                goToStudentInfo()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Veri alınamadı: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goToStudentInfo() {
        val intent = Intent(this, ogrtdevbilgi::class.java)
        intent.putExtra("secilenTarih", selectedDate)
        intent.putExtra("dersID", lessonCode)
        startActivity(intent)
    }
}
