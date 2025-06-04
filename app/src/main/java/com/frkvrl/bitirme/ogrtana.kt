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

                // DataSnapshot'ı User veri sınıfına dönüştür
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    Log.d("FirebaseData", "Kullanıcı ad: ${user.ad}, soyad: ${user.soyad}")
                    textView6.text = user.ad ?: "Ad yok"
                    textViewSoyad.text = user.soyad ?: "Soyad yok"

                    // Dersler alanını manuel olarak doğru tipe dönüştürmeye çalış
                    val derslerMap: Map<String, Map<String, Boolean>> = try {
                        @Suppress("UNCHECKED_CAST") // Güvenli olmayan tip dönüşüm uyarısını gizle
                        user.dersler as? Map<String, Map<String, Boolean>> ?: emptyMap()
                    } catch (e: Exception) {
                        // Dönüştürme sırasında bir hata oluşursa (örn: ArrayList gelirse)
                        Log.e("FirebaseData", "Dersler alanı dönüştürme hatası: ${e.message}", e)
                        emptyMap() // Hata durumunda boş bir Map döndür
                    }
                    // derslerMap'i burada kullanabilirsiniz, örneğin Log'a yazdırarak kontrol edebilirsiniz.
                    Log.d("FirebaseData", "Dersler: $derslerMap")

                } else {
                    Log.w("FirebaseData", "User objesi null veya dönüştürme başarısız.")
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
