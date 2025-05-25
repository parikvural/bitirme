package com.frkvrl.bitirme

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class DevamsizlikListesiActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DevamsizlikAdapter
    private val devamsizlikListesi = mutableListOf<OgrenciYoklama>() // UID ve devamsızlık sayısı

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devamsizlik_listesi)

        val dersID = intent.getStringExtra("dersID") ?: return
        val sinif = intent.getStringExtra("sinif") ?: return // Sınıf bilgisini al

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DevamsizlikAdapter(devamsizlikListesi)
        recyclerView.adapter = adapter

        getAttendanceCounts(dersID, sinif) // Sınıf bilgisini de metoda gönder
    }

    private fun getAttendanceCounts(lessonCode: String, sinif: String) {
        val database =
            FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")
        val attendanceRef = database.getReference("attendances").child(sinif).child(lessonCode) // Sınıf ve ders kodunu path'e ekle
        val usersRef = database.getReference("users")

        attendanceRef.get().addOnSuccessListener { snapshot ->
            val counts = mutableMapOf<String, Int>()

            for (dateSnapshot in snapshot.children) {
                for (studentSnapshot in dateSnapshot.children) {
                    val uid = studentSnapshot.key ?: continue
                    val isPresent = studentSnapshot.getValue(Boolean::class.java) ?: false
                    if (!isPresent) {
                        counts[uid] = (counts[uid] ?: 0) + 1
                    }
                }
            }

            usersRef.get().addOnSuccessListener { usersSnapshot ->
                devamsizlikListesi.clear()

                for ((uid, count) in counts) {
                    val ad = usersSnapshot.child(uid).child("ad").getValue(String::class.java) ?: ""
                    val soyad = usersSnapshot.child(uid).child("soyad").getValue(String::class.java) ?: ""
                    // Düzeltilen satır: Long tipini String'e dönüştürmek için Any ve toString() kullanıldı.
                    val numara = usersSnapshot.child(uid).child("numara").getValue(Any::class.java)?.toString() ?: ""
                    val fullName = if (ad.isNotBlank() && soyad.isNotBlank()) "$ad $soyad" else uid

                    devamsizlikListesi.add(OgrenciYoklama(fullName, numara, false)) // katildiMi: false
                }

                adapter.notifyDataSetChanged()
            }.addOnFailureListener {
                Toast.makeText(
                    this,
                    "Kullanıcı bilgileri alınamadı: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

        }.addOnFailureListener {
            Toast.makeText(this, "Veriler alınamadı: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }
}