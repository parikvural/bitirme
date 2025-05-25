package com.frkvrl.bitirme

import android.content.Intent // Intent için import eklendi
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.* // Date ve Locale için import eklendi

class yoklamalistesi : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("YoklamaListesi", "=== BAŞLANGIČ ===")

        try {
            setContentView(R.layout.activity_yoklamalistesi)
            Log.d("YoklamaListesi", "Layout set edildi")

            // RecyclerView kontrolü
            val recyclerView: RecyclerView? = findViewById(R.id.recyclerView)
            if (recyclerView == null) {
                Log.e("YoklamaListesi", "RecyclerView bulunamadı!")
                Toast.makeText(this, "RecyclerView bulunamadı!", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            recyclerView.layoutManager = LinearLayoutManager(this)
            Log.d("YoklamaListesi", "RecyclerView hazırlandı")

            // Intent verilerini al
            val lessonCode = intent.getStringExtra("dersID")?.trim()
            val sinif = intent.getStringExtra("sinif")?.trim()

            Log.d("YoklamaListesi", "Intent verileri - lessonCode: '$lessonCode', sinif: '$sinif'")

            if (lessonCode.isNullOrEmpty()) {
                Log.e("YoklamaListesi", "lessonCode boş!")
                Toast.makeText(this, "Ders kodu alınamadı.", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            if (sinif.isNullOrEmpty()) {
                Log.e("YoklamaListesi", "sinif boş!")
                Toast.makeText(this, "Sınıf bilgisi alınamadı.", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Basit test listesi göster
            val testList = listOf(
                OgrenciYoklama("Test Öğrenci 1", "123", true),
                OgrenciYoklama("Test Öğrenci 2", "456", false)
            )

            recyclerView.adapter = yoklamaadapter(testList)
            Log.d("YoklamaListesi", "Test adapter set edildi")

            Toast.makeText(this, "Test verileri yüklendi", Toast.LENGTH_SHORT).show()

            // Firebase işlemlerini başlat (2 saniye sonra)
            android.os.Handler().postDelayed({
                loadFirebaseData(lessonCode, sinif, recyclerView)
            }, 2000)

        } catch (e: Exception) {
            Log.e("YoklamaListesi", "onCreate'de crash: ${e.message}")
            Log.e("YoklamaListesi", "Stack trace: ${e.stackTraceToString()}")
            Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadFirebaseData(lessonCode: String, sinif: String, recyclerView: RecyclerView) {
        try {
            Log.d("YoklamaListesi", "Firebase verileri yükleniyor...")

            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            Log.d("YoklamaListesi", "Bugünün tarihi: $todayDate")

            val db = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")
            val lessonRefPath = "dersler/$sinif/$lessonCode/ogrenciler"
            Log.d("YoklamaListesi", "Lesson ref path: $lessonRefPath")

            val lessonRef = db.getReference(lessonRefPath)

            lessonRef.get().addOnSuccessListener { snapshot ->
                Log.d("YoklamaListesi", "Firebase'den yanıt alındı")
                Log.d("YoklamaListesi", "Snapshot exists: ${snapshot.exists()}")
                Log.d("YoklamaListesi", "Children count: ${snapshot.childrenCount}")

                if (!snapshot.exists()) {
                    Toast.makeText(this, "Ders bulunamadı: $lessonRefPath", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val ogrenciUIDs = mutableListOf<String>()
                for (child in snapshot.children) {
                    Log.d("YoklamaListesi", "Child key: ${child.key}, value: ${child.value}")
                    val value = child.getValue(Boolean::class.java)
                    if (value == true) { // Sadece true değerine sahip öğrencileri al
                        child.key?.let { ogrenciUIDs.add(it) }
                    }
                }

                Log.d("YoklamaListesi", "Bulunan UID'ler: $ogrenciUIDs")

                if (ogrenciUIDs.isEmpty()) {
                    Toast.makeText(this, "Derse kayıtlı öğrenci yok", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Şimdi gerçek öğrenci bilgilerini al
                loadStudentDetails(ogrenciUIDs, lessonCode, sinif, todayDate, recyclerView)

            }.addOnFailureListener { error ->
                Log.e("YoklamaListesi", "Firebase hatası: ${error.message}")
                Toast.makeText(this, "Firebase hatası: ${error.message}", Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            Log.e("YoklamaListesi", "loadFirebaseData'da hata: ${e.message}")
            Toast.makeText(this, "Veri yükleme hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadStudentDetails(ogrenciUIDs: List<String>, lessonCode: String, sinif: String, todayDate: String, recyclerView: RecyclerView) {
        try {
            Log.d("YoklamaListesi", "Öğrenci detayları yükleniyor. UID sayısı: ${ogrenciUIDs.size}")

            val db = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")
            val userRef = db.getReference("users")
            val attendanceRef = db.getReference("attendances/$sinif/$lessonCode/$todayDate")

            Log.d("YoklamaListesi", "Attendance path: attendances/$sinif/$lessonCode/$todayDate")

            val ogrenciList = mutableListOf<OgrenciYoklama>()
            var completedCount = 0

            // Önce yoklama durumlarını al
            attendanceRef.get().addOnSuccessListener { attendanceSnapshot ->
                Log.d("YoklamaListesi", "Attendance snapshot exists: ${attendanceSnapshot.exists()}")

                // Her UID için öğrenci bilgilerini al
                ogrenciUIDs.forEach { uid ->
                    Log.d("YoklamaListesi", "UID işleniyor: $uid")

                    val katildiMi = attendanceSnapshot.child(uid).getValue(Boolean::class.java) ?: false
                    Log.d("YoklamaListesi", "UID: $uid, Katıldı mı: $katildiMi")

                    userRef.child(uid).get().addOnSuccessListener { userSnapshot ->
                        Log.d("YoklamaListesi", "User snapshot exists for $uid: ${userSnapshot.exists()}")

                        val ad = userSnapshot.child("ad").getValue(String::class.java) ?: "Bilinmeyen"
                        val soyad = userSnapshot.child("soyad").getValue(String::class.java) ?: ""
                        // Hata veren satır düzeltildi: Long tipindeki değeri String'e dönüştürmek için Any ve toString() kullanıldı.
                        val numara = userSnapshot.child("numara").getValue(Any::class.java)?.toString() ?: "?"

                        Log.d("YoklamaListesi", "User bilgileri - Ad: $ad, Soyad: $soyad, Numara: $numara")

                        ogrenciList.add(OgrenciYoklama("$ad $soyad", numara, katildiMi))
                        completedCount++

                        Log.d("YoklamaListesi", "Completed: $completedCount / ${ogrenciUIDs.size}")

                        if (completedCount == ogrenciUIDs.size) {
                            Log.d("YoklamaListesi", "Tüm öğrenciler yüklendi, adapter güncelleniyor")
                            val sortedList = ogrenciList.sortedBy { it.adSoyad }

                            // runOnUiThread bloğunu try-catch içine al
                            runOnUiThread {
                                try {
                                    recyclerView.adapter = yoklamaadapter(sortedList)
                                    Toast.makeText(this@yoklamalistesi, "Yoklama listesi hazır", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Log.e("YoklamaListesi", "Adapter set etmede veya UI güncellemede hata: ${e.message}")
                                    Toast.makeText(this@yoklamalistesi, "Liste görüntülenirken bir hata oluştu.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }

                    }.addOnFailureListener { userError ->
                        Log.e("YoklamaListesi", "User bilgisi alınamadı ($uid): ${userError.message}")

                        // Hata durumunda da sayacı artır
                        ogrenciList.add(OgrenciYoklama("Bilinmeyen ($uid)", "?", katildiMi))
                        completedCount++

                        if (completedCount == ogrenciUIDs.size) {
                            Log.d("YoklamaListesi", "Tüm öğrenciler yüklendi (bazı hatalarla), adapter güncelleniyor")
                            val sortedList = ogrenciList.sortedBy { it.adSoyad }
                            runOnUiThread {
                                try {
                                    recyclerView.adapter = yoklamaadapter(sortedList)
                                    Toast.makeText(this@yoklamalistesi, "Yoklama listesi hazır (bazı hatalarla)", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Log.e("YoklamaListesi", "Hata durumunda adapter set etmede veya UI güncellemede sorun: ${e.message}")
                                    Toast.makeText(this@yoklamalistesi, "Liste görüntülenirken bir hata oluştu (hata durumunda).", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }

            }.addOnFailureListener { attendanceError ->
                Log.e("YoklamaListesi", "Attendance verisi alınamadı: ${attendanceError.message}")
                Toast.makeText(this, "Yoklama verisi alınamadı: ${attendanceError.message}", Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            Log.e("YoklamaListesi", "loadStudentDetails'da hata: ${e.message}")
            Toast.makeText(this, "Öğrenci detayları yüklenemedi: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
