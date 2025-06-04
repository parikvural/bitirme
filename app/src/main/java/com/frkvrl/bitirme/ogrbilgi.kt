package com.frkvrl.bitirme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class ogrbilgi : ogrnavbar() {
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val dersMap = mutableMapOf<String, String>() // dersAd -> dersId
    private var kullaniciSinif: String? = null // Kullanıcının sınıfını tutacak değişken

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ogrbilgi) // ogrbilgi.xml layout dosyasını yüklüyoruz

        listView = findViewById(R.id.listView)
        // ArrayAdapter'ı özel layout ile başlatın
        adapter = ArrayAdapter(this, R.layout.list_item_white_text, ArrayList())
        listView.adapter = adapter

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val dbRef = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")
            val userSinifRef = dbRef.getReference("users").child(uid).child("sinif")

            userSinifRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val sinifFromFirebase = snapshot.getValue(String::class.java)
                    if (sinifFromFirebase != null) {
                        kullaniciSinif = sinifFromFirebase // Sınıf bilgisini burada saklıyoruz
                        Log.d("OgrBilgi", "Kullanıcı sınıfı Firebase'den alındı: $kullaniciSinif")
                        dersleriYukle(uid, kullaniciSinif!!) // Dersleri sınıf bilgisine göre yüklüyoruz

                        // Sınıf bilgisi alındıktan sonra tıklama dinleyicisini ayarlıyoruz
                        listView.setOnItemClickListener { _, _, position, _ ->
                            val dersAdi = adapter.getItem(position)
                            val dersId = dersMap[dersAdi]

                            if (dersId != null && kullaniciSinif != null) {
                                val intent = Intent(this@ogrbilgi, ogrdevam::class.java)
                                intent.putExtra("dersId", dersId)
                                intent.putExtra("sinif", kullaniciSinif) // Doğru sınıf bilgisini gönderiyoruz
                                startActivity(intent)
                            } else {
                                Toast.makeText(this@ogrbilgi, "Ders veya sınıf bilgisi eksik.", Toast.LENGTH_SHORT).show()
                                Log.e("OgrBilgi", "Ders ID ($dersId) veya Kullanıcı Sınıfı ($kullaniciSinif) null.")
                            }
                        }

                    } else {
                        Log.e("FirebaseError", "Sınıf bilgisi Firebase'den alınamadı veya null.")
                        Toast.makeText(this@ogrbilgi, "Sınıf bilgisi bulunamadı.", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Kullanıcı sınıfı alınamadı: ${error.message}")
                    Toast.makeText(this@ogrbilgi, "Sınıf bilgisi alınamadı: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
        } else {
            Toast.makeText(this, "Kullanıcı oturumu açık değil.", Toast.LENGTH_LONG).show()
            Log.e("OgrBilgi", "Kullanıcı UID null.")
        }
    }

    private fun dersleriYukle(uid: String, sinif: String) {
        val dbRef = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")
        val sinifRef = dbRef.getReference("dersler").child(sinif)

        sinifRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dersMap.clear()
                val yeniDersler = ArrayList<String>()

                for (dersSnapshot in snapshot.children) {
                    val dersId = dersSnapshot.key
                    val dersAdi = dersSnapshot.child("ad").getValue(String::class.java)
                    // Öğrencinin o derse kayıtlı olup olmadığını kontrol ediyoruz
                    val ogrenciKayitli = dersSnapshot.child("ogrenciler").child(uid).getValue(Boolean::class.java) == true

                    if (dersAdi != null && dersId != null && ogrenciKayitli) {
                        dersMap[dersAdi] = dersId
                        yeniDersler.add(dersAdi)
                    }
                }

                adapter.clear()
                adapter.addAll(yeniDersler)
                adapter.notifyDataSetChanged()

                if (yeniDersler.isEmpty()) {
                    Toast.makeText(this@ogrbilgi, "Kayıtlı ders bulunamadı.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Dersler alınamadı: ${error.message}")
                Toast.makeText(this@ogrbilgi, "Dersler yüklenirken hata oluştu: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
