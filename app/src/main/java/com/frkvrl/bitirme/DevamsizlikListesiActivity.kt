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
    private val devamsizlikListesi = mutableListOf<OgrenciYoklama>() // İsim, numara ve devamsızlık sayısı

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devamsizlik_listesi)

        val dersID = intent.getStringExtra("dersID") ?: return
        val sinif = intent.getStringExtra("sinif") ?: return

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DevamsizlikAdapter(devamsizlikListesi)
        recyclerView.adapter = adapter

        getAttendanceCounts(dersID, sinif)
    }

    private fun getAttendanceCounts(lessonCode: String, sinif: String) {
        val database =
            FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")
        val attendanceRef = database.getReference("attendances").child(sinif).child(lessonCode)
        val usersRef = database.getReference("users")

        attendanceRef.get().addOnSuccessListener { snapshot ->
            val attendanceCounts = mutableMapOf<String, Int>() // UID -> Devamsızlık Sayısı

            for (dateSnapshot in snapshot.children) {
                for (studentSnapshot in dateSnapshot.children) {
                    val uid = studentSnapshot.key ?: continue
                    val isPresent = studentSnapshot.getValue(Boolean::class.java) ?: false
                    if (!isPresent) {
                        attendanceCounts[uid] = (attendanceCounts[uid] ?: 0) + 1
                    }
                }
            }

            usersRef.get().addOnSuccessListener { usersSnapshot ->
                devamsizlikListesi.clear()

                // Devamsızlığı olan öğrencileri listeye ekle
                for ((uid, devamsizlikSayisi) in attendanceCounts) {
                    val userRole = usersSnapshot.child(uid).child("rol").getValue(String::class.java)
                    // Sadece rolü "Öğrenci" olanları ekle
                    if (userRole == "Öğrenci") {
                        val ad = usersSnapshot.child(uid).child("ad").getValue(String::class.java) ?: ""
                        val soyad = usersSnapshot.child(uid).child("soyad").getValue(String::class.java) ?: ""
                        val numara = usersSnapshot.child(uid).child("numara").getValue(Any::class.java)?.toString() ?: ""
                        val fullName = if (ad.isNotBlank() && soyad.isNotBlank()) "$ad $soyad" else uid
                        devamsizlikListesi.add(OgrenciYoklama(fullName, numara, devamsizlikSayisi))
                    }
                }

                // Tüm öğrencilerin listesini alıp devamsızlığı olmayanları da ekleyelim
                // Sadece rolü "Öğrenci" olanları filtrele
                val allStudentsInClass = mutableMapOf<String, OgrenciBilgi>()
                for (userSnapshot in usersSnapshot.children) {
                    val uid = userSnapshot.key ?: continue
                    val userRole = userSnapshot.child("rol").getValue(String::class.java)
                    val userSinif = userSnapshot.child("sinif").getValue(String::class.java)

                    // Sadece rolü "Öğrenci" ve ilgili sınıfa ait olanları al
                    if (userRole == "Öğrenci" && userSinif == sinif) {
                        val ad = userSnapshot.child("ad").getValue(String::class.java) ?: ""
                        val soyad = userSnapshot.child("soyad").getValue(String::class.java) ?: ""
                        val numara = userSnapshot.child("numara").getValue(Any::class.java)?.toString() ?: ""
                        allStudentsInClass[uid] = OgrenciBilgi(ad, soyad, numara)
                    }
                }

                for ((uid, ogrenciBilgi) in allStudentsInClass) {
                    val fullName = if (ogrenciBilgi.ad.isNotBlank() && ogrenciBilgi.soyad.isNotBlank()) "${ogrenciBilgi.ad} ${ogrenciBilgi.soyad}" else uid
                    val devamsizlikSayisi = attendanceCounts[uid] ?: 0
                    // Sadece listede henüz olmayan öğrencileri ekle (devamsızlığı 0 olanlar dahil)
                    if (!devamsizlikListesi.any { it.numara == ogrenciBilgi.numara }) { // Numaraya göre kontrol daha güvenli
                        devamsizlikListesi.add(OgrenciYoklama(fullName, ogrenciBilgi.numara, devamsizlikSayisi))
                    }
                }

                // Listeyi devamsızlık sayısına göre sıralayabilirsiniz (isteğe bağlı)
                devamsizlikListesi.sortByDescending { it.devamsizlikSayisi }

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

data class OgrenciBilgi(val ad: String, val soyad: String, val numara: String)

