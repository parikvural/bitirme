package com.frkvrl.bitirme

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ograna : ogrnavbar() {

    private lateinit var textViewAd: TextView
    private lateinit var textViewSoyad: TextView
    private lateinit var textViewNumara: TextView
    private lateinit var textViewBolum: TextView
    private lateinit var textViewSinif: TextView

    private val database = Firebase.database("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app")
    private val usersRef = database.getReference("users")

    private var userValueEventListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.ograna)

        // TextView'leri bağla
        textViewAd = findViewById(R.id.textView6)
        textViewSoyad = findViewById(R.id.textView7)
        textViewNumara = findViewById(R.id.textView15)
        textViewBolum = findViewById(R.id.textView3)
        textViewSinif = findViewById(R.id.textView5)

        startFetchingUser()
    }

    private fun startFetchingUser() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUid == null) {
            Log.w("FirebaseData", "Giriş yapan kullanıcı yok.")
            return
        }

        val currentUserRef = usersRef.child(currentUid)

        userValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Log.w("FirebaseData", "Kullanıcı verisi bulunamadı.")
                    textViewAd.text = "Ad yok"
                    textViewSoyad.text = "Soyad yok"
                    textViewNumara.text = "Numara yok"
                    textViewBolum.text = "Bölüm yok"
                    textViewSinif.text = "Sınıf yok"
                    return
                }

                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    textViewAd.text = user.ad ?: "Ad yok"
                    textViewSoyad.text = user.soyad ?: "Soyad yok"
                    textViewNumara.text = user.numara?.toString() ?: "Numara yok"
                    textViewBolum.text = user.bolum ?: "Bölüm yok"
                    textViewSinif.text = user.sinif?.toString() ?: "Sınıf yok"

                    Log.d("FirebaseData", "Kullanıcı bilgileri güncellendi.")
                } else {
                    Log.w("FirebaseData", "User objesi null.")

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("FirebaseData", "Veri alınamadı: ${error.toException()}")
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
