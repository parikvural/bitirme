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
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ArrayList(dersMap.keys))
        listView.adapter = adapter

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (!uid.isNullOrEmpty()) {
            val dbRef = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")
            val lessonsRef = dbRef.getReference("lessons")

            lessonsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    dersMap.clear()

                    for (lessonSnapshot in snapshot.children) {
                        val dersId = lessonSnapshot.key
                        val dersAdi = lessonSnapshot.child("ad").getValue(String::class.java)
                        val ogrenciVar = lessonSnapshot.child("ogrenciler").child(uid).getValue(Boolean::class.java)

                        if (dersAdi != null && ogrenciVar == true && dersId != null) {
                            dersMap[dersAdi] = dersId
                        }
                    }

                    adapter.clear()
                    adapter.addAll(dersMap.keys)
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Ders listesi alınamadı: ${error.message}")
                }
            })
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val dersAdi = adapter.getItem(position)
            val dersId = dersMap[dersAdi]

            val intent = Intent(this, ogrdevam::class.java)
            intent.putExtra("dersId", dersId)
            startActivity(intent)
        }
    }
}
