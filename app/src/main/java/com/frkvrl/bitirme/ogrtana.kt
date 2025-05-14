package com.frkvrl.bitirme

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ogrtana : ogrtnavbar() {

    private lateinit var textViewAd: TextView
    private lateinit var textViewSoyad: TextView

    private val database = Firebase.database("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app")
    private val usersRef = database.getReference("users")

    private val userValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid

            if (currentUid != null) {
                for (userSnap in snapshot.children) {
                    val user = userSnap.getValue(User::class.java)

                    if (user != null && user.uid == currentUid) {
                        // TextView'lere verileri yerleştir
                        textViewAd.text = user.ad ?: "Ad yok"
                        textViewSoyad.text = user.soyad ?: "Soyad yok"


                        Log.d("FirebaseData", "Kullanıcı bilgileri güncellendi.")
                        break
                    }
                }
            } else {
                Log.w("FirebaseData", "Giriş yapan kullanıcı yok.")
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.w("FirebaseData", "Veri alınamadı: ${error.toException()}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ogrtana)

        // TextView'leri bağla
        textViewAd = findViewById(R.id.textViewAd)
        textViewSoyad = findViewById(R.id.textViewSoyad)

        startFetchingUser()
    }

    private fun startFetchingUser() {
        usersRef.addValueEventListener(userValueEventListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        usersRef.removeEventListener(userValueEventListener)
    }
}
