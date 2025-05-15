package com.frkvrl.bitirme

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        val verdigiDerslerRef = database.getReference("users/$currentUid/verdigi_dersler")

        verdigiDerslerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(this@ogrtders, "Ders bulunamadı", Toast.LENGTH_SHORT).show()
                    return
                }

                for (dersSnapshot in snapshot.children) {
                    val dersID = dersSnapshot.key ?: continue

                    // Ders adı için lessons/{dersID}/ad
                    val dersAdRef = database.getReference("lessons/$dersID/ad")
                    dersAdRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(adSnapshot: DataSnapshot) {
                            val dersAdi = adSnapshot.getValue(String::class.java) ?: dersID

                            val textView = TextView(this@ogrtders).apply {
                                text = dersAdi
                                textSize = 18f
                                setPadding(20, 20, 20, 20)
                                setOnClickListener {
                                    val intent = Intent(this@ogrtders, ogrtqr::class.java)
                                    intent.putExtra("dersID", dersID)
                                    startActivity(intent)
                                }
                            }
                            dersListLayout.addView(textView)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@ogrtders, "Hata: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ogrtders, "Veritabanı hatası", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
