package com.frkvrl.bitirme

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase

class giris : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var realtimeDb: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.giris)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        realtimeDb = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")

        val emailEdit = findViewById<EditText>(R.id.edit)
        val passwordEdit = findViewById<EditText>(R.id.edit2)
        val btnKayit = findViewById<TextView>(R.id.button3)
        val btnSifremiUnuttum = findViewById<TextView>(R.id.textView8)

        btnKayit.setOnClickListener {
            startActivity(Intent(this, kayit::class.java))
        }

        btnSifremiUnuttum.setOnClickListener {
            val email = emailEdit.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Lütfen e-posta girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
                if (task.isSuccessful && task.result.signInMethods?.isNotEmpty() == true) {
                    auth.sendPasswordResetEmail(email).addOnSuccessListener {
                        Toast.makeText(this, "Şifre sıfırlama bağlantısı gönderildi.", Toast.LENGTH_LONG).show()
                    }.addOnFailureListener {
                        Toast.makeText(this, "Gönderim başarısız: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Bu e-posta ile kayıtlı kullanıcı bulunamadı.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Eğer kullanıcı zaten giriş yaptıysa direkt yönlendir
        auth.currentUser?.let { user ->
            val uid = user.uid
            kontrolVeYonlendir(uid)
        }
    }

    fun giris(view: View) {
        val email = findViewById<EditText>(R.id.edit).text.toString().trim()
        val password = findViewById<EditText>(R.id.edit2).text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email ve şifre giriniz", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) {
                    cihazKontrolVeKayit(uid)
                } else {
                    Toast.makeText(this, "Kullanıcı bilgisi alınamadı.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Giriş başarısız: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun cihazKontrolVeKayit(uid: String) {
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val cihazRef = firestore.collection("device_links").document(deviceId)

        cihazRef.get().addOnSuccessListener { doc ->
            val kayitliUid = doc.getString("uid")

            if (kayitliUid == null) {
                // İlk kez giriş yapan kullanıcı için cihaz kaydı yapılıyor
                cihazRef.set(mapOf("uid" to uid)).addOnSuccessListener {
                    kontrolVeYonlendir(uid)
                }.addOnFailureListener {
                    Toast.makeText(this, "Cihaz eşleştirme kaydı başarısız!", Toast.LENGTH_SHORT).show()
                }
            } else if (kayitliUid == uid) {
                // Aynı kullanıcı tekrar giriş yapıyor
                kontrolVeYonlendir(uid)
            } else {
                // Başka kullanıcı bu cihazdan giriş yapamaz
                Toast.makeText(this, "Bu cihaz başka kullanıcıya kayıtlı!", Toast.LENGTH_LONG).show()
                auth.signOut()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Firestore hatası: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun kontrolVeYonlendir(uid: String) {
        val userRef = realtimeDb.getReference("users/$uid")

        userRef.get().addOnSuccessListener { snapshot ->
            val rol = snapshot.child("rol").getValue(String::class.java)

            when (rol) {
                "Öğrenci" -> {
                    startActivity(Intent(this, ograna::class.java))
                    finish()
                }
                "Öğretmen" -> {
                    startActivity(Intent(this, ogrtana::class.java))
                    finish()
                }
                else -> {
                    Toast.makeText(this, "Kullanıcı rolü tanımsız", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Kullanıcı bilgisi alınamadı: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
