package com.frkvrl.bitirme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ogrbilgi : ogrnavbar() {
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val dersMap = mutableMapOf<String, String>() // dersAd -> dersId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ogrbilgi)

        listView = findViewById(R.id.listView)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ArrayList())
        listView.adapter = adapter

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val dbRef = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")
            val userSinifRef = dbRef.getReference("users").child(uid).child("sinif")

            userSinifRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val kullaniciSinif = snapshot.getValue(String::class.java)
                    if (kullaniciSinif != null) {
                        dersleriYukle(uid, kullaniciSinif)
                    } else {
                        Log.e("FirebaseError", "Sınıf bilgisi alınamadı")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Kullanıcı sınıfı alınamadı: ${error.message}")
                }
            })
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val dersAdi = adapter.getItem(position)
            val dersId = dersMap[dersAdi]

            val intent = Intent(this@ogrbilgi, ogrdevam::class.java)
            intent.putExtra("dersId", dersId)
            intent.putExtra("sinif", "bilinmiyor") // Sınıf bilgisini intent'e ekleyin (gerekliyse)
            startActivity(intent)
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
                    val ogrenciVar = dersSnapshot.child("ogrenciler").child(uid).getValue(Boolean::class.java) == true

                    if (dersAdi != null && dersId != null && ogrenciVar) {
                        dersMap[dersAdi] = dersId
                        yeniDersler.add(dersAdi)
                    }
                }

                adapter.clear()
                adapter.addAll(yeniDersler)
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Dersler alınamadı: ${error.message}")
            }
        })
    }
}