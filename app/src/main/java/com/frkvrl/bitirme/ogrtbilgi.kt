package com.frkvrl.bitirme

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class ogrtbilgi : ogrtnavbar() {

    private lateinit var lessonCode: String
    private lateinit var selectedDate: String // Seçilen tarihi tutacak değişken
    private lateinit var sinif: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ogrtbilgi)

        try {
            lessonCode = intent.getStringExtra("dersID") ?: "BILINMEYEN"
            selectedDate = getTodayDate() // Her zaman bugünün tarihi olarak ayarlanır
            sinif = intent.getStringExtra("sinif") ?: "0"

            Log.d("OgrtBilgi", "onCreate - lessonCode: $lessonCode, selectedDate: $selectedDate, sinif: $sinif")
        } catch (e: Exception) {
            Log.e("OgrtBilgi", "Hata oluştu: ${e.message}", e)
            Toast.makeText(this, "Başlatma hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }

        // Tarih seçme butonunu görünmez yap veya kaldır
        findViewById<Button>(R.id.btnDatePicker).visibility = View.GONE

        findViewById<Button>(R.id.button5).visibility = View.GONE

        findViewById<Button>(R.id.button2).setOnClickListener {
            val intent = Intent(this, DevamsizlikListesiActivity::class.java)
            intent.putExtra("dersID", lessonCode)
            intent.putExtra("sinif", sinif)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnQrBaslat).setOnClickListener {
            val attendanceRef = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("attendances/$sinif/$lessonCode/$selectedDate")

            attendanceRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var attendanceTaken = false
                    for (studentSnapshot in snapshot.children) {
                        val isPresent = studentSnapshot.getValue(Boolean::class.java)
                        if (isPresent == true) {
                            attendanceTaken = true
                            break
                        }
                    }

                    if (attendanceTaken) {
                        val messageTextView = TextView(this@ogrtbilgi).apply {
                            text = "Bugün ($selectedDate) için yoklama zaten alınmış. Yeni QR kod oluşturulamaz."
                            textSize = 16f
                            setTextColor(ContextCompat.getColor(this@ogrtbilgi, R.color.onPrimary))
                            setPadding(48, 48, 48, 48)
                        }

                        AlertDialog.Builder(this@ogrtbilgi, R.style.AlertDialogCustom)
                            .setTitle("Yoklama Alınmış")
                            .setView(messageTextView)
                            .setPositiveButton("Tamam", null)
                            .show()
                            .also { dialog ->
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                    ?.setTextColor(ContextCompat.getColor(this@ogrtbilgi, R.color.onPrimary))
                            }
                    } else {
                        startQrSession()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("OgrtBilgi", "Yoklama kontrol hatası: ${error.message}")
                    Toast.makeText(this@ogrtbilgi, "Yoklama kontrol hatası", Toast.LENGTH_LONG).show()
                }
            })
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
                val tempCalendar = Calendar.getInstance()
                tempCalendar.set(selectedYear, selectedMonth, selectedDay)
                val selectedDateAsLong = tempCalendar.timeInMillis

                val todayCalendar = Calendar.getInstance()
                todayCalendar.set(todayCalendar.get(Calendar.YEAR), todayCalendar.get(Calendar.MONTH), todayCalendar.get(Calendar.DAY_OF_MONTH))
                val todayDateAsLong = todayCalendar.timeInMillis

                if (selectedDateAsLong < todayDateAsLong) {
                    // Seçilen tarih bugünden eskiyse uyarı ver
                    val messageTextView = TextView(this@ogrtbilgi).apply {
                        text = "Geçmiş bir tarih için yoklama oluşturulamaz."
                        textSize = 16f
                        setTextColor(ContextCompat.getColor(this@ogrtbilgi, R.color.onPrimary))
                        setPadding(48, 48, 48, 48)
                    }

                    AlertDialog.Builder(this@ogrtbilgi, R.style.AlertDialogCustom)
                        .setTitle("Geçersiz Tarih")
                        .setView(messageTextView)
                        .setPositiveButton("Tamam", null)
                        .show()
                        .also { dialog ->
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                ?.setTextColor(ContextCompat.getColor(this@ogrtbilgi, R.color.onPrimary))
                        }
                } else {
                    // Tarih geçerliyse güncelle ve yoklamayı oluştur
                    selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                    Toast.makeText(this, "Seçilen tarih: $selectedDate", Toast.LENGTH_SHORT).show()
                    createAttendanceIfNotExists()
                }
            }, year, month, day)

        // DatePickerDialog'da bugünden önceki tarihleri devre dışı bırak
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000 // Geçmişi seçmeyi engelle
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
                    // JSON yapınızda dersler altında sınıf numaraları (1, 2 vb.) var.
                    // Bu nedenle, dersGroup'u doğrudan sinif'e göre filtrelemeliyiz.
                    val dersGroup = derslerSnapshot.child(sinif) // Doğrudan sınıfı hedefle
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
                    }


                    if (!found) {
                        Toast.makeText(this, "Ders bulunamadı: $lessonCode (Sınıf: $sinif)", Toast.LENGTH_LONG).show()
                        Log.e("OgrtBilgi", "Ders bulunamadı veya sınıf bilgisi yanlış: Ders Kodu: $lessonCode, Sınıf: $sinif")
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Ders bilgisi alınamadı", Toast.LENGTH_SHORT).show()
                    Log.e("OgrtBilgi", "Ders bilgisi alınamadı Firebase hatası: ${it.message}")
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Veri alınamadı: ${it.message}", Toast.LENGTH_SHORT).show()
            Log.e("OgrtBilgi", "Yoklama varlık kontrolü Firebase hatası: ${it.message}")
        }
    }

    private fun startQrSession() {
        val database = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app")
        val qrRef = database.getReference("qrCodes/current")

        val qrData = mapOf(
            "value" to UUID.randomUUID().toString().take(8),
            "timestamp" to System.currentTimeMillis(),
            "lessonCode" to lessonCode,
            "sinif" to sinif // sinif bilgisini QR oturumuna iletiyoruz
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
        intent.putExtra("sinif", sinif) // sinif bilgisini ogrtqr'ye iletiyoruz
        startActivity(intent)
    }
}
