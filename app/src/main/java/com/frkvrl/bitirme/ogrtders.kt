package com.frkvrl.bitirme

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ogrtders : ogrtnavbar() {

    private lateinit var dersListLayout: LinearLayout
    private val database = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ogrtders)

        dersListLayout = findViewById(R.id.dersListLayout)

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid == null) {
            Toast.makeText(this, "Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }

        // Tüm sınıflardaki dersleri dolaş
        val derslerRef = database.getReference("dersler")
        derslerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (sinifSnapshot in snapshot.children) {
                    val sinif = sinifSnapshot.key ?: continue

                    for (dersSnapshot in sinifSnapshot.children) {
                        val dersID = dersSnapshot.key ?: continue
                        val dersData = dersSnapshot.value as? Map<*, *> ?: continue

                        val ogretmenUid = dersSnapshot.child("ogretmen_uid").getValue(String::class.java)
                        if (ogretmenUid == currentUid) {
                            val dersAdi = dersSnapshot.child("ad").getValue(String::class.java) ?: dersID

                            val textView = TextView(this@ogrtders).apply {
                                text = "$dersAdi ($sinif.Sınıf)"
                                textSize = 18f
                                setPadding(20, 20, 20, 20)
                                setOnClickListener {
                                    val intent = Intent(this@ogrtders, ogrtbilgi::class.java)
                                    intent.putExtra("dersID", dersID)
                                    intent.putExtra("sinif", sinif)
                                    startActivity(intent)
                                }
                            }

                            dersListLayout.addView(textView)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ogrtders, "Veritabanı hatası: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
