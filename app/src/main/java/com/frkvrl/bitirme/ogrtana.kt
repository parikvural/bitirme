package com.frkvrl.bitirme

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ogrtana : ogrtnavbar() {

    private lateinit var textView6: TextView
    private lateinit var textViewSoyad: TextView

    private val database = Firebase.database("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app")
    private val usersRef = database.getReference("users")

    private var userValueEventListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ogrtana)

        textView6 = findViewById(R.id.textView6)
        textViewSoyad = findViewById(R.id.textViewSoyad)

        startFetchingUser()
    }

    private fun startFetchingUser() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("FirebaseData", "Current UID: $currentUid")
        if (currentUid == null) {
            Log.w("FirebaseData", "Giriş yapan kullanıcı yok.")
            textView6.text = "Ad yok"
            textViewSoyad.text = "Soyad yok"
            return
        }

        val currentUserRef = usersRef.child(currentUid)

        userValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Log.w("FirebaseData", "Kullanıcı verisi bulunamadı.")
                    textView6.text = "Ad yok"
                    textViewSoyad.text = "Soyad yok"
                    return
                }

                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    Log.d("FirebaseData", "Kullanıcı ad: ${user.ad}, soyad: ${user.soyad}")
                    textView6.text = user.ad ?: "Ad yok"
                    textViewSoyad.text = user.soyad ?: "Soyad yok"
                } else {
                    Log.w("FirebaseData", "User objesi null.")
                    textView6.text = "Ad yok"
                    textViewSoyad.text = "Soyad yok"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("FirebaseData", "Veri alınamadı: ${error.toException()}")
                textView6.text = "Ad yok"
                textViewSoyad.text = "Soyad yok"
            }
        }
        currentUserRef.addValueEventListener(userValueEventListener as ValueEventListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null && userValueEventListener != null) {
            usersRef.child(currentUid).removeEventListener(userValueEventListener as ValueEventListener)
        }
    }
}
