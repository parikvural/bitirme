package com.frkvrl.bitirme

import android.os.Bundle
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ogrdevam : ogrnavbar() {

    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ogrdevam)

        textView = findViewById(R.id.textViewDersBilgi)

        val dersId = intent.getStringExtra("dersId")
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (dersId != null && uid != null) {
            val database = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")
            val attendanceRef = database.getReference("attendances/$dersId")

            attendanceRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var katildigi = 0
                    var toplam = 0

                    for (dateSnapshot in snapshot.children) {
                        toplam++
                        val katilim = dateSnapshot.child(uid).getValue(Boolean::class.java) ?: false
                        if (katilim) katildigi++
                    }

                    val devamsizlik = toplam - katildigi
                    textView.text = "Toplam yoklama: $toplam\nKatıldığınız: $katildigi\nDevamsızlık: $devamsizlik"
                }

                override fun onCancelled(error: DatabaseError) {
                    textView.text = "Veri alınamadı: ${error.message}"
                }
            })
        } else {
            textView.text = "Ders bilgisi alınamadı."
        }
    }
}
